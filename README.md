# QuizApp com Firebase

Aplicativo Android de quiz com autenticação, criação de quizzes, histórico de resultados e ranking global.

## Funcionalidades

- Cadastro e login com `Firebase Authentication` (e-mail e senha).
- CRUD de quizzes (criar, editar e excluir).
- Execução de quiz com temporizador por minutos.
- Registro de pontuação por tentativa.
- Histórico do usuário com métricas (total, acertos e média).
- Ranking global (top 5) com base na média de acertos.
- Cache local com `Room` para suporte offline:
  - Quizzes e perguntas sincronizados do Firebase.
  - Resultados salvos localmente.

## Stack

- Kotlin
- Android SDK (`minSdk 24`, `targetSdk 36`, `compileSdk 36`)
- ViewBinding
- Firebase Realtime Database
- Firebase Authentication
- Room + KSP
- Coroutines

## Estrutura resumida

- `app/src/main/java/com/example/myapplicationquiz/`
  - `LoginActivity.kt`: autenticação e persistência de perfil.
  - `MainActivity.kt`: listagem/CRUD de quizzes, dashboard e ranking.
  - `QuizActivity.kt`: execução do quiz e cálculo de score.
  - `HistoryActivity.kt`: histórico e estatísticas do usuário.
  - `local/`: entidades, DAOs e banco Room.
- `app/src/main/res/layout/`: layouts das telas e diálogos.

## Pré-requisitos

- Android Studio (recomendado: versão mais recente estável)
- JDK 11
- Projeto Firebase configurado com:
  - Authentication (Email/Password)
  - Realtime Database
- Arquivo `google-services.json` dentro de `app/`

## Como rodar

1. Clone o repositório.
2. Abra no Android Studio.
3. Verifique se o arquivo `app/google-services.json` está correto para seu projeto Firebase.
4. Sincronize o Gradle.
5. Execute em emulador ou dispositivo físico.

## Build por linha de comando

```bash
./gradlew assembleDebug
```

APK gerado em:

`app/build/outputs/apk/debug/`

## Banco e sincronização

- Fonte principal: Firebase (`Quizzes`, `Results`, `Users`).
- Cache local: banco Room `quiz_app_db`.
- Ao abrir a tela principal, os quizzes da nuvem são sincronizados para o banco local.
- Em falha de rede, o app exibe quizzes salvos localmente.

## Observações

- O fluxo de login redireciona automaticamente para a tela principal quando já existe sessão ativa.
- Há um `app-debug.apk` versionado na raiz do projeto, útil para testes rápidos.
