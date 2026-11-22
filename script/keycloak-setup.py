#!/usr/bin/env python3
"""
Prépare Keycloak pour la plateforme (idempotent) :
  - realm lorrconnect importé depuis realm-export.json s'il n'existe pas
  - clients publics du front (chabeille, cuvedesaulnes) s'ils n'existent pas
  - rôles CLIENT et ADMIN sur le client haller (lus par les microservices dans resource_access.haller)
  - compte APP_USER / APP_PASSWORD avec les deux rôles
  - attribut utilisateur numeroClient (exigé par commande-service pour passer commande) :
    déclaré dans le profil utilisateur, copié dans les jetons des clients du front,
    renseigné sur le compte (APP_NUMERO_CLIENT, défaut CLI-00001) s'il ne l'est pas déjà
Usage : APP_USER=... APP_PASSWORD=... python3 keycloak-setup.py <realm-export.json>
"""
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

KC = os.environ.get("KC_URL", "http://localhost:30090")
REALM = "lorrconnect"
FRONT = "http://localhost:30080"
PUBLIC_URL = os.environ.get("PUBLIC_URL", "").rstrip("/")
# Origines autorisées pour les clients du front : local, plus l'adresse publique si elle est définie
FRONT_ORIGINS = [FRONT] + ([PUBLIC_URL] if PUBLIC_URL else [])
FRONT_CLIENTS = ["chabeille", "cuvedesaulnes"]
ROLES = ["CLIENT", "ADMIN"]
NUMERO_CLIENT_ATTR = "numeroClient"


def call(method, path, token=None, body=None, form=None):
    headers = {}
    data = None
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode()
    if form is not None:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        data = urllib.parse.urlencode(form).encode()
    req = urllib.request.Request(KC + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        return e.code, None


def admin_token():
    for _ in range(36):  # jusqu'à 3 minutes : Keycloak met du temps au premier démarrage
        try:
            status, data = call("POST", "/realms/master/protocol/openid-connect/token", form={
                "grant_type": "password", "client_id": "admin-cli",
                "username": os.environ.get("KC_ADMIN", "admin"),
                "password": os.environ.get("KC_ADMIN_PASSWORD", "admin")})
            if status == 200:
                return data["access_token"]
        except OSError:
            pass
        time.sleep(5)
    sys.exit("✘ Keycloak injoignable sur " + KC)


def client_uuid(token, client_id):
    _, found = call("GET", f"/admin/realms/{REALM}/clients?clientId={client_id}", token)
    return found[0]["id"] if found else None


def main():
    export_file = sys.argv[1]
    user, password = os.environ["APP_USER"], os.environ["APP_PASSWORD"]
    token = admin_token()

    # 1. Realm
    status, _ = call("GET", f"/admin/realms/{REALM}", token)
    if status == 404:
        with open(export_file, encoding="utf-8") as f:
            realm = json.load(f)
        status, _ = call("POST", "/admin/realms", token, realm)
        print(f"  + realm {REALM} importé" if status == 201 else f"  ✘ import du realm : HTTP {status}")
    else:
        print(f"  = realm {REALM}")

    # 2. Clients publics du front (créés, ou complétés avec les origines manquantes)
    redirects = [o + "/*" for o in FRONT_ORIGINS]
    for cid in FRONT_CLIENTS:
        uuid = client_uuid(token, cid)
        if not uuid:
            status, _ = call("POST", f"/admin/realms/{REALM}/clients", token, {
                "clientId": cid, "enabled": True, "publicClient": True,
                "standardFlowEnabled": True, "directAccessGrantsEnabled": False,
                "redirectUris": redirects, "webOrigins": FRONT_ORIGINS,
                "attributes": {"post.logout.redirect.uris": "+", "pkce.code.challenge.method": "S256"}})
            print(f"  + client {cid}" if status == 201 else f"  ✘ client {cid} : HTTP {status}")
            continue
        _, client = call("GET", f"/admin/realms/{REALM}/clients/{uuid}", token)
        missing_r = [r for r in redirects if r not in client.get("redirectUris", [])]
        missing_o = [o for o in FRONT_ORIGINS if o not in client.get("webOrigins", [])]
        if missing_r or missing_o:
            client["redirectUris"] = client.get("redirectUris", []) + missing_r
            client["webOrigins"] = client.get("webOrigins", []) + missing_o
            status, _ = call("PUT", f"/admin/realms/{REALM}/clients/{uuid}", token, client)
            print(f"  ✓ client {cid} : ajout de {', '.join(missing_r + missing_o)}" if status == 204
                  else f"  ✘ client {cid} : HTTP {status}")
        else:
            print(f"  = client {cid}")

    # 3. Rôles sur le client haller
    haller = client_uuid(token, "haller")
    if not haller:
        sys.exit("✘ client haller introuvable dans le realm")
    for role in ROLES:
        status, _ = call("POST", f"/admin/realms/{REALM}/clients/{haller}/roles", token, {"name": role})
        print(f"  + rôle {role}" if status == 201 else f"  = rôle {role}")

    # 4. Attribut numeroClient : déclaré dans le profil utilisateur (Keycloak 24+ refuse sinon
    #    les attributs non déclarés), puis copié dans les jetons des clients du front
    status, profile = call("GET", f"/admin/realms/{REALM}/users/profile", token)
    if status == 200 and profile is not None:
        if any(a.get("name") == NUMERO_CLIENT_ATTR for a in profile.get("attributes", [])):
            print(f"  = attribut {NUMERO_CLIENT_ATTR}")
        else:
            profile.setdefault("attributes", []).append({
                "name": NUMERO_CLIENT_ATTR, "displayName": "Numéro client", "multivalued": False,
                "permissions": {"view": ["admin", "user"], "edit": ["admin"]}})
            status, _ = call("PUT", f"/admin/realms/{REALM}/users/profile", token, profile)
            print(f"  + attribut {NUMERO_CLIENT_ATTR}" if status == 200 else f"  ✘ attribut {NUMERO_CLIENT_ATTR} : HTTP {status}")
    for cid in FRONT_CLIENTS:
        uuid = client_uuid(token, cid)
        if not uuid:
            continue
        _, mappers = call("GET", f"/admin/realms/{REALM}/clients/{uuid}/protocol-mappers/models", token)
        if any(m.get("config", {}).get("claim.name") == NUMERO_CLIENT_ATTR for m in (mappers or [])):
            print(f"  = mapper {NUMERO_CLIENT_ATTR} sur {cid}")
            continue
        status, _ = call("POST", f"/admin/realms/{REALM}/clients/{uuid}/protocol-mappers/models", token, {
            "name": NUMERO_CLIENT_ATTR, "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-attribute-mapper",
            "config": {"user.attribute": NUMERO_CLIENT_ATTR, "claim.name": NUMERO_CLIENT_ATTR,
                       "jsonType.label": "String", "access.token.claim": "true",
                       "id.token.claim": "true", "userinfo.token.claim": "true"}})
        print(f"  + mapper {NUMERO_CLIENT_ATTR} sur {cid}" if status == 201 else f"  ✘ mapper sur {cid} : HTTP {status}")

    # 5. Compte de connexion
    status, _ = call("POST", f"/admin/realms/{REALM}/users", token, {
        "username": user, "email": user, "firstName": "Admin", "lastName": "Local",
        "enabled": True, "emailVerified": True,
        "credentials": [{"type": "password", "value": password, "temporary": False}]})
    created = status == 201
    print(f"  + compte {user}" if created else f"  = compte {user}")
    _, users = call("GET", f"/admin/realms/{REALM}/users?username={urllib.parse.quote(user)}&exact=true", token)
    user_id = users[0]["id"]
    # Mot de passe réaligné sur la valeur connue (APP_PASSWORD ou .app-credentials) ; jamais remplacé
    # par un mot de passe tiré au hasard sur un compte existant (KC_RESET_PASSWORD=0)
    if not created and os.environ.get("KC_RESET_PASSWORD", "1") == "1":
        status, _ = call("PUT", f"/admin/realms/{REALM}/users/{user_id}/reset-password", token,
                         {"type": "password", "value": password, "temporary": False})
        print("  ✓ mot de passe à jour" if status == 204 else f"  ✘ mot de passe : HTTP {status}")
    elif not created:
        print("  = mot de passe inchangé (défini depuis une autre machine)")
    roles = []
    for role in ROLES:
        _, r = call("GET", f"/admin/realms/{REALM}/clients/{haller}/roles/{role}", token)
        roles.append({"id": r["id"], "name": r["name"]})
    call("POST", f"/admin/realms/{REALM}/users/{user_id}/role-mappings/clients/{haller}", token, roles)
    print("  ✓ rôles CLIENT + ADMIN attribués")

    # 6. Numéro client du compte : renseigné seulement s'il est absent (une valeur saisie à la main est gardée)
    _, account = call("GET", f"/admin/realms/{REALM}/users/{user_id}", token)
    attrs = account.get("attributes") or {}
    if attrs.get(NUMERO_CLIENT_ATTR):
        print(f"  = {NUMERO_CLIENT_ATTR} : {attrs[NUMERO_CLIENT_ATTR][0]}")
    else:
        numero = os.environ.get("APP_NUMERO_CLIENT", "CLI-00001")
        attrs[NUMERO_CLIENT_ATTR] = [numero]
        account["attributes"] = attrs
        status, _ = call("PUT", f"/admin/realms/{REALM}/users/{user_id}", token, account)
        print(f"  + {NUMERO_CLIENT_ATTR} : {numero}" if status == 204 else f"  ✘ {NUMERO_CLIENT_ATTR} : HTTP {status}")


if __name__ == "__main__":
    main()
