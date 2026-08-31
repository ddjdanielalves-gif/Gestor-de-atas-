# 📋 Gestor de Atas & Prazos (Android + Web)

Plataforma completa para receber, interpretar atas de reuniões, extrair decisões/responsáveis e alertar sobre prazos de deliberações e lembretes de agenda.

Disponível tanto como **Aplicativo Android (.APK)** quanto como **Aplicação Web (HTML/JS/Node)**.

---

## 🌐 Como Rodar ou Hospedar a Versão Web

### Opção 1: No Render.com
1. No painel do Render, crie um **Static Site** ou **Web Service** conectado a este repositório do GitHub.
2. Configure:
   - **Language / Environment**: `Static Site` ou `Node`
   - **Build Command**: `npm run build` *(ou deixar em branco)*
   - **Publish Directory**: `.` *(ponto / raiz)*
3. Clique em **Create**. O Render gerará seu link público gratuito (ex: `https://gestor-de-atas.onrender.com`).

### Opção 2: No GitHub Pages (100% Gratuito)
1. No seu repositório no GitHub, vá em **Settings** > **Pages**.
2. Em **Source**, selecione `Deploy from a branch` -> `main` / `/ (root)`.
3. Salve e acesse pelo link gerado pelo GitHub!

---

## 📱 Como Baixar o APK Diretamente pelo GitHub

Este repositório já possui uma automação configurada com **GitHub Actions** que gera o arquivo `.apk` automaticamente.

### Passo a passo para baixar no seu celular:
1. Abra este repositório no GitHub pelo navegador do celular ou computador.
2. Clique na aba superior **"Actions"** (Ações).
3. Clique na execução mais recente de **"Gerar APK Android"** (ou no workflow verde com visto de sucesso).
4. Na parte inferior da página, na seção **"Artifacts"** (Artefatos), clique em **`GestorDeAtas-debug-apk`**.
5. O download do arquivo compactado contendo o `app-debug.apk` será iniciado.
6. Descompacte ou abra o `.apk` no seu celular Android e instale!

---

## 🚀 Funcionalidades Principais

- **Recepção e Interpretação de Atas**:
  - Extração de temas centrais, deliberações e nomes dos responsáveis designados.
  - Ajuste rápido de prazos (3, 7, 14, 30 dias ou datas personalizadas).
- **Painel de Alertas & Vencimentos**:
  - Alertas prioritários na consulta: *Vencidas (Atrasadas)*, *Vencendo Hoje* e *Próximos Dias*.
  - Filtros por responsável e por status.
- **Agenda de Lembretes**:
  - Sincronização de compromissos e lembretes para a data da consulta.
- **Armazenamento 100% Local**:
  - Total privacidade e funcionamento offline (LocalStorage na Web / SQLite Room no Android).

---

## 🛠️ Tecnologias Utilizadas

- **Kotlin** & **Jetpack Compose** (Material Design 3)
- **Room Database** (Persistência local)
- **ViewModel & Kotlin Coroutines/StateFlow**
- **GitHub Actions** (CI/CD para compilação automática de APK)
