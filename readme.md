# 🧾 Services Platform — Commande / Devis / Facturation / Comptabilité

## 📌 Présentation
Ce projet est une plateforme **modulaire basée sur microservices** construite avec **Spring Boot 4 + Java 21**.

Elle permet de gérer un cycle complet de gestion commerciale :

- 🛒 Commandes clients
- 🧾 Devis commerciaux
- 📄 Facturation
- 📊 Comptabilité (TVA + reporting)
- 📁 Export PDF & Excel

---

# 🧱 Architecture globale

```
    
 
    ─────────────────────────────────────────────                  
     │                │                        │                      
┌──────────┐     ┌──────────────┐      ┌──────────────┐   
│ CLIENT   │     │  API GATEWAY │      │ REGISTRATION │                   
│ UI       │     │              │      │  AUTH JWT    │
│          │     │              │      │  (key cloak) │  
└──────────┘     └────┬─────────┘      └──────────────┘   
   ──────────────REST + EVENTS + JWT───────────────   
     │                │                        │
┌─────▼──────┐  ┌─────▼──────┐         ┌───────▼──┐
│            │  │  ACTUATOR  │         │ EUREKA   │
│ MESSAGING  │  │            │         │ DISCOVERY│
└────┬───────┘  └─────┬──────┘         └─────┬────┘     
     └────────────────+──────────────────────┘
                      │
        ┌──────────────────────────────┐
        │ MICROSERVICES BACKEND        │
        │ commande / devis / produit   │
        │ client (postGreSQL)          │
        └──────────────────────────────┘
```

---

# 📦 Microservices

## 🔐 registration auth-service with Key cloak
- connection
- JWT generation
- rafraichir les tokens
- gestions des roles et droits
- enregistrement utilisateurs
- email verification
- account activation
- gestion mot de passe

## 🧭 discovery-service Eureka
- enregistre tous les microservices

## 🔁 api-gateway
- point d'entrée centralisé
- distribue les appels vers les microservices
- valide les autorisations et accès

## 🛒 commande-service
- Liste de commandes (admin)
- Créer une commande (client)
- Voir le détail d'une commande (admin / client)
- Annuler une commande (admin / client)


## 🧾 devis-service
- Liste de devis (admin)
- Créer un devis (admin)
- Voir le détail d'un dévis (admin/client)


## 📄 facture-service
- Liste de factures (admin)
- Créer une facture (admin)
- Voir le détail d'une facture (admin/client)

## 📊 compta-service
- Déclaration TVA (admin)
- Export Excel comptable (admin)
- Reporting financier (admin)
- Calcul chiffre d’affaires (admin)

---

# 🖥️ Interfaces

## 👤 Client UI
- Passer des commandes (client)
- List des commandes (client / admin)
- Consulter commandes (client / admin)


# 🔄 Flux métier
```
Commande → Devis → Facture → Comptabilité
```

---

# 🌍 Fonctionnalités globales

- 🌐 Multi-langue (FR / EN / DE / LB)
- 📄 Export PDF (devis & facture)
- 📊 Export Excel comptable
- 🧾 TVA reporting


# 🧠 Architecture technique

- Java 21
- Spring Boot 4
- Angular 21
- Spring Cloud Gateway
- Spring Security (JWT)
- PostgreSQL 
- Apache POI (Excel)
- PDF generator (OpenPDF / iText alternative)
- Kafka / RabbitMQ (event-driven optionnel)

---

# 🗄 Base de données

Base de données H2/POSTGRESQL:

- commande-db
- devis-db
- facture-db
- compta-db
- auth-db

---

# Deployment tools

- kubernetes
- docker
- kind

