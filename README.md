# 🚀 Istikbal SAP Portal - Enterprise Dynamic Excel & n8n AI Automation Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![n8n Integration](https://img.shields.io/badge/n8n-Automation-FF6D5A?style=for-the-badge&logo=n8n&logoColor=white)](https://n8n.io/)
[![JWT Security](https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203.0-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)

**Istikbal SAP Portal**, SAP sistemlerinden alınan dinamik Excel raporlarının PostgreSQL veritabanına aktarılmasını, dinamik kolon ve veri sayfalamasını (pagination), **n8n otomasyonu** üzerinden e-posta gönderimini ve **AI LLM modelleri (Gemini / OpenAI)** ile otomatik Excel veri analizini sağlayan kurumsal bir web platformudur.

---

## 🌟 Öne Çıkan Özellikler

### 📊 1. Dinamik SAP Excel Yükleme & PostgreSQL Veritabanı
- Her türlü sütun yapısına sahip `.xlsx` / `.xls` dosyalarını dinamik olarak ayrıştırır.
- Başlıkları (headers) ve satırları PostgreSQL veritabanına JSON formatında kaydeder.
- Sayfalama (Pagination), dinamik arama (Search) ve sunucu taraflı sıralama desteği.

### 🤖 2. n8n AI LLM Veri Analizörü (AI Excel Analyzer)
- Yüklenen Excel verilerini ve hesaplanan sayısal istatistikleri n8n üzerindeki AI Agent / LLM düğümlerine (`/api/ai/analyze`) iletir.
- Yönetici Özeti (Executive Summary), Risk Analizi ve Sayısal İstatistik Raporu üretir.
- Oluşturulan AI analiz raporunu tek tıkla e-posta taslağına aktarma ve n8n ile gönderme imkanı.

### ✉️ 3. n8n Otomatik E-Posta Gönderim Paneli
- Kayıtlı alıcı listesi yönetimi (PostgreSQL `email_recipients` tablosu).
- n8n Webhook (`/api/mail/send`) üzerinden `HTTP POST` ile anlık ve senkronize e-posta gönderimi.
- Gönderim durumunun (Başarılı / Başarısız) arayüzde canlı bildirimi.

### 🔐 4. Güvenlik & Mimari
- **JWT (JSON Web Token)** tabanlı kimlik doğrulama.
- Role tabanlı erişim kontrolü (`ROLE_ADMIN`, `ROLE_USER`).
- **Swagger UI** entegrasyonu (`/swagger-ui/index.html`) ile tam API dokümantasyonu.

---

## 🏗️ Sistem Mimarisi

```text
[ Frontend UI (HTML5/CSS3/JS) ] 
            │
            ▼
[ Spring Boot REST Controllers ]
    ├── Auth & Security (JWT)
    ├── SAP Report & Dynamic Excel Service
    ├── Email Recipient Service
    └── AI Analysis Service
            │
    ┌───────┴───────────────────────┐
    ▼                               ▼
[ PostgreSQL DB ]        [ n8n Automation Engine ]
  • Dynamic Excel Data     ├── Webhook: /api/mail/send (Gmail Node)
  • Metadata               └── Webhook: /api/ai/analyze (AI LLM Agent)
  • Email Recipients
```

---

## 🛠️ Teknolojiler & Kütüphaneler

- **Backend:** Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, Hibernate
- **Veritabanı:** PostgreSQL 14+
- **Excel İşleme:** Apache POI 5.x
- **Güvenlik:** JWT (io.jsonwebtoken / jjwt 0.11.5)
- **Otomasyon & AI:** n8n Workflow Engine, LangChain AI Agent, Google Gemini / OpenAI LLM
- **Frontend:** Responsive Dashboard (HTML5, CSS3, Vanilla JavaScript, FontAwesome 6)
- **Dokümantasyon:** OpenAPI 3.0 / Springdoc Swagger UI

---

## 🚀 Kurulum ve Çalıştırma Rehberi

### 1. Ön Gereksinimler
- Java 17 veya üzeri (`JDK 17`)
- PostgreSQL Veritabanı
- Node.js & n8n (Opsiyonel - Otomasyon akışları için)

### 2. Veritabanı Yapılandırması
PostgreSQL üzerinde `istikbal` şemasını oluşturun:

```sql
CREATE DATABASE postgres;
CREATE SCHEMA IF NOT EXISTS istikbal;
```

`src/main/resources/application.properties` dosyasındaki veritabanı bağlantı bilgilerini düzenleyin:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres?currentSchema=istikbal
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

### 3. Projeyi Derleme ve Çalıştırma

```bash
# Projeyi derleyin
./mvnw clean compile

# Sunucuyu başlatın
./mvnw spring-boot:run
```

Sunucu varsayılan olarak **`http://localhost:8080`** adresinde çalışmaya başlayacaktır.

---

## 🔗 n8n Webhook Entegrasyonu

n8n üzerinde aşağıdaki 2 adet Webhook akışını oluşturmanız önerilir:

### ✉️ 1. E-Posta Gönderim Akışı (`/api/mail/send`)
- **Webhook Node:** `POST` `/api/mail/send` (Respond: `When Last Node Finishes`)
- **Gmail Node:** 
  - To: `={{ $json.body.email }}`
  - Subject: `={{ $json.body.title }}`
  - Message: `={{ $json.body.message }}`

### 🤖 2. AI Analiz Akışı (`/api/ai/analyze`)
- **Webhook Node:** `POST` `/api/ai/analyze` (Respond: `When Last Node Finishes`)
- **AI Agent Node:** Gemini 1.5 Flash / GPT-4o-mini
- **Yanıt Formatı:** `{ "analysis": "AI Rapor Metni..." }`

---

## 📡 REST API Endpointleri

| Metod | Endpoint | Açıklama |
|---|---|---|
| `POST` | `/api/auth/login` | JWT Kullanıcı Girişi |
| `POST` | `/api/auth/register` | Yeni Kullanıcı Kaydı |
| `POST` | `/api/excel/upload` | Dinamik Excel Yükleme & Parsing |
| `GET` | `/api/excel/dynamic` | Sayfalı Dinamik Excel Verisi Getirme |
| `GET` | `/api/mail/recipients` | Kayıtlı E-Posta Alıcı Listesi |
| `POST` | `/api/mail/send` | n8n Üzerinden E-Posta Gönderimi |
| `POST` | `/api/ai/analyze` | n8n AI ile Excel Analizi Başlatma |

---

## 📑 Swagger Dokümantasyonu

Uygulama çalıştıktan sonra Swagger API arayüzüne şu adresten erişebilirsiniz:  
👉 **`http://localhost:8080/swagger-ui/index.html`**

---

## 👤 Lisans & İletişim

**Geliştirici:** Emre Şahin  
**Proje:** Istikbal SAP Portal Automation System  
