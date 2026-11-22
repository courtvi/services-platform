#!/usr/bin/env python3
# =============================================================================
# Génère k8s/observability/grafana-dashboards.yml : les tableaux de bord Grafana provisionnés
# dans otel-lgtm (dossier « services-platform »).
#   - Golden signals : trafic, erreurs, latence, saturation par service
#   - SLI / SLO      : disponibilité et latence, budget d'erreur, burn rate
#
# Usage : python3 k8s/observability/gen-dashboards.py   (puis redéployer)
# Les tableaux provisionnés ne sont pas modifiables durablement depuis Grafana : on change ce script.
# =============================================================================
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "grafana-dashboards.yml")

DS = {"type": "prometheus", "uid": "${datasource}"}

# Métriques de l'agent OpenTelemetry (http.server.request.duration, en secondes)
REQ = "http_server_request_duration_seconds_count"
BUCKET = "http_server_request_duration_seconds_bucket"
# Les sondes Kubernetes (/manage/...) ne sont pas du trafic utilisateur : exclues partout
NOPROBE = 'http_route!~"/manage.*"'
SEL = '{job=~"$job", ' + NOPROBE + '}'
SEL_5XX = '{job=~"$job", ' + NOPROBE + ', http_response_status_code=~"5.."}'
SEL_4XX = '{job=~"$job", ' + NOPROBE + ', http_response_status_code=~"4.."}'


def var_datasource():
    return {"name": "datasource", "label": "Source", "type": "datasource", "query": "prometheus",
            "current": {}, "hide": 0}


def var_job(multi, default):
    current = {"text": default, "value": default}
    if multi:
        current = {"text": ["All"], "value": ["$__all"]}
    return {"name": "job", "label": "Service", "type": "query", "datasource": DS,
            "query": {"query": f"label_values({REQ}, job)", "refId": "job"},
            "definition": f"label_values({REQ}, job)", "refresh": 2, "sort": 1,
            "multi": multi, "includeAll": multi, "current": current, "hide": 0}


def var_custom(name, label, options, default):
    return {"name": name, "label": label, "type": "custom", "query": ",".join(options),
            "current": {"text": default, "value": default},
            "options": [{"text": o, "value": o, "selected": o == default} for o in options], "hide": 0}


def thresholds(steps):
    return {"mode": "absolute",
            "steps": [{"color": c, "value": v} for c, v in steps]}


GREEN_ONLY = thresholds([("green", None)])


def target(expr, legend="", ref="A", instant=False):
    t = {"datasource": DS, "expr": expr, "legendFormat": legend, "refId": ref, "range": not instant}
    if instant:
        t["instant"] = True
    return t


class Layout:
    def __init__(self):
        self.y = 0
        self.x = 0
        self.row_h = 0
        self.pid = 0

    def place(self, w, h):
        if self.x + w > 24:
            self.y += self.row_h
            self.x, self.row_h = 0, 0
        pos = {"x": self.x, "y": self.y, "w": w, "h": h}
        self.x += w
        self.row_h = max(self.row_h, h)
        return pos

    def newline(self):
        if self.x:
            self.y += self.row_h
            self.x, self.row_h = 0, 0

    def next_id(self):
        self.pid += 1
        return self.pid


def row(L, title):
    L.newline()
    p = {"type": "row", "title": title, "id": L.next_id(), "collapsed": False, "panels": [],
         "gridPos": {"x": 0, "y": L.y, "w": 24, "h": 1}}
    L.y += 1
    return p


def stat(L, title, expr, unit, th=GREEN_ONLY, decimals=None, desc="", w=6, h=4):
    d = {"unit": unit, "thresholds": th, "color": {"mode": "thresholds"}}
    if decimals is not None:
        d["decimals"] = decimals
    return {"type": "stat", "title": title, "description": desc, "id": L.next_id(), "datasource": DS,
            "gridPos": L.place(w, h), "targets": [target(expr, instant=True)],
            "fieldConfig": {"defaults": d, "overrides": []},
            "options": {"reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
                        "colorMode": "background", "graphMode": "none", "textMode": "value",
                        "justifyMode": "center", "orientation": "auto"}}


def ts(L, title, targets, unit, desc="", w=12, h=8, th=None, min_=None, max_=None):
    d = {"unit": unit, "color": {"mode": "palette-classic"},
         "custom": {"lineWidth": 2, "fillOpacity": 10, "showPoints": "never", "spanNulls": True,
                    "thresholdsStyle": {"mode": "dashed" if th else "off"}}}
    if th:
        d["thresholds"] = th
    if min_ is not None:
        d["min"] = min_
    if max_ is not None:
        d["max"] = max_
    tg = [target(e, lg, chr(ord("A") + i)) for i, (e, lg) in enumerate(targets)]
    return {"type": "timeseries", "title": title, "description": desc, "id": L.next_id(), "datasource": DS,
            "gridPos": L.place(w, h), "targets": tg,
            "fieldConfig": {"defaults": d, "overrides": []},
            "options": {"legend": {"displayMode": "table", "placement": "bottom",
                                   "calcs": ["lastNotNull", "max"]},
                        "tooltip": {"mode": "multi", "sort": "desc"}}}


def dashboard(uid, title, desc, panels, variables, time_from="now-6h"):
    return {"uid": uid, "title": title, "description": desc, "tags": ["services-platform"],
            "timezone": "browser", "schemaVersion": 39, "version": 1, "editable": True,
            "refresh": "30s", "time": {"from": time_from, "to": "now"},
            "templating": {"list": variables}, "annotations": {"list": []}, "panels": panels}


# -----------------------------------------------------------------------------
# Golden signals
# -----------------------------------------------------------------------------
def rate_req(sel, by="job", w="5m"):
    return f"sum by ({by}) (rate({REQ}{sel}[{w}]))"


def quantile(q, by="job"):
    return f"histogram_quantile({q}, sum by ({by}, le) (rate({BUCKET}{SEL}[5m])))"


HEAP_USED = '{__name__=~"jvm_memory_used_bytes|jvm_memory_used", jvm_memory_type="heap", job=~"$job"}'
HEAP_LIMIT = '{__name__=~"jvm_memory_limit_bytes|jvm_memory_limit", jvm_memory_type="heap", job=~"$job"}'
CPU = '{__name__=~"jvm_cpu_recent_utilization_ratio|jvm_cpu_recent_utilization", job=~"$job"}'
THREADS = '{__name__=~"jvm_thread_count|process_runtime_jvm_threads_count", job=~"$job"}'


def golden():
    L = Layout()
    P = []
    P.append(row(L, "Vue d'ensemble (services sélectionnés)"))
    P.append(stat(L, "Trafic", f"sum(rate({REQ}{SEL}[5m]))", "reqps", decimals=2,
                  desc="Requêtes par seconde (hors sondes Kubernetes), moyenne sur 5 min"))
    P.append(stat(L, "Erreurs 5xx", f"(sum(rate({REQ}{SEL_5XX}[5m])) or vector(0)) / sum(rate({REQ}{SEL}[5m]))",
                  "percentunit", thresholds([("green", None), ("orange", 0.01), ("red", 0.05)]), 2,
                  desc="Part des requêtes en erreur serveur (5xx) sur 5 min"))
    P.append(stat(L, "Latence p95", f"histogram_quantile(0.95, sum by (le) (rate({BUCKET}{SEL}[5m])))", "s",
                  thresholds([("green", None), ("orange", 0.5), ("red", 1)]), 3,
                  desc="95 % des requêtes répondent en moins de cette durée (5 min)"))
    P.append(stat(L, "Saturation mémoire (max)",
                  f"max(sum by (job, instance) ({HEAP_USED}) / sum by (job, instance) ({HEAP_LIMIT}))",
                  "percentunit", thresholds([("green", None), ("orange", 0.75), ("red", 0.9)]), 1,
                  desc="Instance la plus chargée : heap utilisé / heap maximum"))

    P.append(row(L, "Trafic et erreurs"))
    P.append(ts(L, "Trafic par service", [(rate_req(SEL), "{{job}}")], "reqps",
                "Requêtes par seconde, hors sondes Kubernetes"))
    P.append(ts(L, "Taux d'erreur 5xx par service",
                [(f"(sum by (job) (rate({REQ}{SEL_5XX}[5m])) or (sum by (job) (rate({REQ}{SEL}[5m])) * 0))"
                  f" / sum by (job) (rate({REQ}{SEL}[5m]))", "{{job}}")],
                "percentunit", "Erreurs serveur : ce qui consomme le budget d'erreur", min_=0,
                th=thresholds([("green", None), ("red", 0.01)])))
    P.append(ts(L, "Erreurs 4xx par service", [(rate_req(SEL_4XX), "{{job}}")], "reqps",
                "Erreurs client (401, 403, 404…) : souvent normales, à surveiller en cas de pic"))
    P.append(ts(L, "Trafic par code HTTP", [(rate_req(SEL, "http_response_status_code"), "{{http_response_status_code}}")],
                "reqps", "Répartition des réponses par code HTTP"))

    P.append(row(L, "Latence"))
    P.append(ts(L, "Latence p95 par service", [(quantile(0.95), "{{job}}")], "s",
                "95 % des requêtes répondent en moins de cette durée",
                th=thresholds([("green", None), ("red", 0.5)]), min_=0))
    P.append(ts(L, "Latence p50 / p95 / p99 (services sélectionnés)",
                [(f"histogram_quantile({q}, sum by (le) (rate({BUCKET}{SEL}[5m])))", lg)
                 for q, lg in [(0.5, "p50"), (0.95, "p95"), (0.99, "p99")]], "s", min_=0))
    P.append(ts(L, "Latence p95 par route", [(quantile(0.95, "job, http_route"), "{{job}} {{http_route}}")], "s",
                "Les routes les plus lentes", w=24, min_=0))

    P.append(row(L, "Saturation"))
    P.append(ts(L, "CPU par instance", [(f"max by (job, instance) ({CPU})", "{{instance}}")], "percentunit",
                "Utilisation CPU récente de la JVM", w=8, min_=0,
                th=thresholds([("green", None), ("red", 0.8)])))
    P.append(ts(L, "Heap par instance", [(f"sum by (job, instance) ({HEAP_USED}) / sum by (job, instance) ({HEAP_LIMIT})",
                                          "{{instance}}")], "percentunit", "Heap utilisé / heap maximum",
                w=8, min_=0, max_=1, th=thresholds([("green", None), ("red", 0.9)])))
    P.append(ts(L, "Threads par instance", [(f"max by (job, instance) ({THREADS})", "{{instance}}")], "short",
                w=8, min_=0))
    P.append(ts(L, "Connexions Postgres utilisées",
                [("sum by (database) (pg_stat_activity_count) / max by (database) (pg_settings_max_connections)",
                  "{{database}}")], "percentunit", "Connexions ouvertes / max_connections, par base",
                min_=0, max_=1, th=thresholds([("green", None), ("red", 0.8)])))
    P.append(ts(L, "Disponibilité des bases (exporteurs)", [("pg_up", "{{database}}")], "short",
                "1 = l'exporteur joint la base", min_=0, max_=1))

    return dashboard("sp-golden-signals", "Golden signals", "Trafic, erreurs, latence et saturation par service",
                     P, [var_datasource(), var_job(True, "All")])


# -----------------------------------------------------------------------------
# SLI / SLO
# -----------------------------------------------------------------------------
def err_ratio(w):
    return f"((sum(increase({REQ}{SEL_5XX}[{w}])) or vector(0)) / sum(increase({REQ}{SEL}[{w}])))"


def slow_ratio(w):
    # requêtes plus lentes que le seuil = 1 - (requêtes sous le seuil / total)
    return (f"(1 - (sum(increase({BUCKET}{{job=~\"$job\", {NOPROBE}, le=\"$seuil\"}}[{w}]))"
            f" / sum(increase({REQ}{SEL}[{w}]))))")


BUDGET_DISPO = "(1 - $slo_dispo / 100)"
BUDGET_LAT = "(1 - $slo_latence / 100)"
BURN_TH = thresholds([("green", None), ("orange", 6), ("red", 14.4)])
BUDGET_TH = thresholds([("red", None), ("orange", 0.25), ("green", 0.5)])
SLI_TH = thresholds([("red", None), ("orange", 0.99), ("green", 0.995)])


def slo_section(L, P, name, ratio, budget, slo_var, sli_desc):
    P.append(row(L, name))
    P.append(stat(L, f"SLI {name.lower()} ($fenetre)", f"1 - {ratio('$fenetre')}", "percentunit", SLI_TH, 3,
                  sli_desc, w=5))
    P.append(stat(L, "Objectif (SLO)", f"vector(${slo_var} / 100)", "percentunit", GREEN_ONLY, 2, w=4))
    P.append(stat(L, "Budget d'erreur restant", f"1 - {ratio('$fenetre')} / {budget}", "percentunit", BUDGET_TH, 1,
                  "Part du budget d'erreur encore disponible sur la fenêtre. Négatif : SLO non tenu.", w=5))
    P.append(stat(L, "Burn rate 1 h", f"{ratio('1h')} / {budget}", "suffix: ×", BURN_TH, 1,
                  "Vitesse de consommation du budget sur la dernière heure. 1 = budget épuisé pile en fin de "
                  "fenêtre ; ≥ 14,4 = alerte rapide (2 % du budget de 30 jours brûlé en 1 h)", w=5))
    P.append(stat(L, "Burn rate 6 h", f"{ratio('6h')} / {budget}", "suffix: ×", BURN_TH, 1,
                  "Sur 6 h ; ≥ 6 = alerte (5 % du budget de 30 jours brûlé en 6 h)", w=5))
    P.append(ts(L, f"SLI {name.lower()} glissant (1 h) et objectif",
                [(f"1 - {ratio('1h')}", "SLI (1 h)"), (f"vector(${slo_var} / 100)", "objectif")],
                "percentunit", max_=1))
    P.append(ts(L, "Burn rate (1 h et 6 h)",
                [(f"{ratio('1h')} / {budget}", "1 h"), (f"{ratio('6h')} / {budget}", "6 h")], "suffix: ×",
                "Seuils : 6 (6 h) et 14,4 (1 h), méthode multi-fenêtres du SRE workbook de Google",
                th=thresholds([("green", None), ("orange", 6), ("red", 14.4)]), min_=0))


def sli_slo():
    L = Layout()
    P = []
    slo_section(L, P, "Disponibilité", err_ratio, BUDGET_DISPO, "slo_dispo",
                "Part des requêtes sans erreur serveur (5xx), sondes exclues")
    slo_section(L, P, "Latence", slow_ratio, BUDGET_LAT, "slo_latence",
                "Part des requêtes servies en moins de $seuil s, sondes exclues")
    variables = [var_datasource(), var_job(False, "api-gateway"),
                 var_custom("slo_dispo", "SLO disponibilité (%)", ["99", "99.5", "99.9"], "99.5"),
                 var_custom("slo_latence", "SLO latence (%)", ["95", "99", "99.5"], "99"),
                 var_custom("seuil", "Seuil de latence (s)", ["0.1", "0.25", "0.5", "1"], "0.5"),
                 var_custom("fenetre", "Fenêtre du SLO", ["7d", "30d"], "30d")]
    return dashboard("sp-sli-slo", "SLI / SLO",
                     "Objectifs de service : disponibilité et latence, budget d'erreur et burn rate. "
                     "Par défaut sur api-gateway, le point d'entrée des utilisateurs.",
                     P, variables, time_from="now-24h")


# -----------------------------------------------------------------------------
def indent(text, n):
    pad = " " * n
    return "\n".join(pad + line if line else line for line in text.splitlines())


def main():
    dashboards = {"golden-signals.json": golden(), "sli-slo.json": sli_slo()}
    provider = """apiVersion: 1
providers:
  - name: services-platform
    folder: services-platform
    type: file
    disableDeletion: true
    allowUiUpdates: false
    updateIntervalSeconds: 30
    options:
      path: /otel-lgtm/grafana/conf/provisioning/dashboards/custom
      foldersFromFilesStructure: false
"""
    parts = ["""# =============================================================================
# FICHIER GÉNÉRÉ par k8s/observability/gen-dashboards.py : ne pas modifier à la main.
# Tableaux de bord Grafana provisionnés dans otel-lgtm (dossier « services-platform »).
# Grafana relit les tableaux toutes les 30 s : pas besoin de redémarrer le pod.
# =============================================================================
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboards-provider
  namespace: lorrconnect
data:
  custom.yaml: |
""" + indent(provider, 4), """---
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboards
  namespace: lorrconnect
data:"""]
    for name, d in dashboards.items():
        parts[-1] += f"\n  {name}: |\n" + indent(json.dumps(d, indent=2, ensure_ascii=False), 4)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(parts) + "\n")
    print(f"écrit : {OUT}")


if __name__ == "__main__":
    main()
