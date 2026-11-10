# AGENTS.md — Wifi GPS (MVP · Fase 1)

Contexto persistente para agents de código. Leia este arquivo inteiro antes de gerar ou alterar qualquer código.

## 1. O que é o projeto

App Android que guarda redes Wi-Fi salvas junto com o local de cada uma e mostra qual é a rede mais próxima, **sem depender de internet**. O usuário vê a distância e uma seta de direção em tempo real para chegar até a rede.

Fase 1 (este MVP): cadastro **manual**, distribuído para amigos fora da Play Store.

## 2. Stack (não trocar sem pedir)

| Área | Escolha |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose (**nunca XML layouts**) |
| Arquitetura | MVVM + Repository |
| Banco local | Room (com KSP, não kapt) |
| Localização | Fused Location Provider (Google Play Services) |
| Criptografia | Android Keystore + AES-GCM (APIs do SDK, sem lib extra) |
| DI | Hilt |
| Navegação | Navigation Compose |
| Build | Gradle Kotlin DSL (`.kts`) |
| Min SDK | API 26 (Android 8.0) |

Fluxo de dependência:

```
UI (Composables) → ViewModel → Repository → Room / GPS
```

- A UI nunca acessa Room, Keystore ou GPS diretamente.
- ViewModels expõem estado via `StateFlow` e recebem dependências via Hilt (`@HiltViewModel`).
- Repositories são a única porta de entrada para dados.

## 3. Escopo do MVP

### Entra

- **RF01** — cadastrar rede (SSID, local, observações).
- **RF02** — definir o local pelo GPS atual **ou** digitando as coordenadas.
- **RF03** — listar, editar e excluir redes.
- **RF04/RF05** — mostrar a rede mais próxima e ordenar a lista por distância.
- **RF06a** — tela com distância e seta de direção em tempo real.
- **RF07** — exportar e importar a lista em JSON, com opção de **excluir as senhas** do arquivo.
- **RF13–RF15** — campo de senha opcional, oculto por padrão, com botão de copiar.

### Fica fora (NÃO implementar, nem como "adiantamento")

- Mapa offline (RF06b) e botão de abrir em app de mapas (RF06c).
- Conectar direto pelo app (RF16) e bloqueio por biometria (RF17).
- Captura automática de redes (Fase 2) e publicação na Play Store (Fase 3).
- Busca por endereço (exigiria internet; só entra se for decidido depois).

Se uma tarefa parecer exigir algo da lista "fora", pare e pergunte.

## 4. Requisitos não funcionais

- **100% offline**: nenhuma chamada de rede. Não adicionar permissão `INTERNET` sem aprovação explícita.
- **Senha criptografada no aparelho** e **fora do backup automático** (ver seção 7).
- Funcionar em API 26+.

## 5. Estrutura de pacotes

Pacote base: `com.moura.wifigps` (ajuste para o real).

```
com.moura.wifigps
├── WifiGpsApplication.kt        (@HiltAndroidApp)
├── MainActivity.kt              (@AndroidEntryPoint, host do NavHost)
├── di/                          (módulos Hilt: DatabaseModule, LocationModule, CryptoModule)
├── data/
│   ├── db/                      (AppDatabase, WifiNetworkDao)
│   ├── model/                   (WifiNetwork)
│   ├── repository/              (WifiRepository)
│   └── export/                  (serialização JSON de export/import)
├── ui/
│   ├── navigation/              (rotas e NavHost)
│   ├── list/                    (lista de redes + rede mais próxima)
│   ├── form/                    (cadastro e edição)
│   ├── compass/                 (distância + seta, RF06a)
│   └── theme/
└── util/
    ├── crypto/                  (CryptoManager: Keystore + AES-GCM)
    ├── location/                (LocationProvider sobre o Fused Location)
    └── geo/                     (distância, bearing, formatação)
```

## 6. Modelo de dados

Entidade única. Já aprovada — **não alterar campos sem pedir**.

```kotlin
@Entity(
    tableName = "wifi_network",
    indices = [
        Index(value = ["ssid"]),                    // busca por nome
        Index(value = ["latitude", "longitude"])    // filtro por região (futuro)
    ]
)
data class WifiNetwork(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // RF01
    val ssid: String,                         // obrigatório
    val notes: String = "",

    // RF02
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String = "",           // ex.: "Casa da vó", "Trabalho"

    // RF13–RF15
    val passwordEncrypted: String? = null,    // null = sem senha cadastrada

    // metadados
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

Regras:

- `passwordEncrypted` é **nullable**: rede sem senha = `null`, nunca string vazia.
- O campo guarda o texto **já criptografado** (ex.: Base64 de IV + ciphertext). Texto puro nunca vai para o Room.
- Atualizar `updatedAt` em toda edição.
- Ordenação por distância é feita em memória (Kotlin), a partir da posição atual — não em SQL.

## 7. Segurança da senha

- Algoritmo: **AES-GCM** com chave no **Android Keystore** (chave não exportável).
- IV novo e aleatório a cada criptografia; armazenar junto do ciphertext.
- Criptografar/descriptografar somente dentro de `CryptoManager`, chamado pelo Repository.
- A UI recebe a senha em texto puro **apenas** no momento em que o usuário pede para exibir ou copiar.
- Senha oculta por padrão na UI (campo mascarado + botão de revelar).
- **Backup**: configurar `android:allowBackup="false"` ou regras em `backup_rules.xml` / `data_extraction_rules.xml` que excluam o banco e as preferências de criptografia. A chave do Keystore não migra entre aparelhos, então uma senha restaurada via backup seria ilegível mesmo.
- Nunca logar senha, texto criptografado ou chave (nem em `Log.d`).
- **Export JSON (RF07)**: o usuário escolhe se inclui ou não as senhas. Ao incluir, exportar a senha descriptografada em texto puro e **avisar claramente na UI** que o arquivo fica sem proteção. Ao importar, criptografar de novo antes de salvar.

## 8. Localização e navegação (RF02, RF04–RF06a)

- Usar `FusedLocationProviderClient`.
- Permissão `ACCESS_FINE_LOCATION`, solicitada em tempo de execução, com fluxo claro para "negada" e "negada permanentemente".
- Posição atual para cadastro: leitura única de alta precisão.
- Tela de seta/distância: atualizações contínuas só enquanto a tela estiver visível (iniciar/parar com o ciclo de vida, via `Flow`).
- Distância: `Location.distanceBetween` ou haversine. Direção: bearing até o destino **menos** o azimute do aparelho (sensor de rotação), para a seta girar corretamente.
- Mostrar distância em metros abaixo de 1 km e em km acima disso.
- Tratar GPS desligado, sem sinal e precisão ruim com mensagens na UI, sem crash.

## 9. Convenções de código

- Kotlin idiomático, sem `!!` (use `?.`, `?:`, `requireNotNull` com mensagem).
- Funções de DAO de leitura retornam `Flow`; escrita usa `suspend`.
- Coroutines: nada de `GlobalScope`; usar `viewModelScope` e injetar `Dispatcher` quando precisar.
- Estado de tela como `data class UiState` exposto por `StateFlow`; eventos de uso único via canal/flow separado.
- Composables: stateless sempre que possível (estado sobe, eventos descem), com `@Preview` nos principais.
- Strings visíveis ao usuário em `res/values/strings.xml` (idioma padrão: **pt-BR**). Nada de texto fixo no código.
- Nomes de código em inglês; comentários e textos de UI em português.
- Versões de dependências ficam em `gradle/libs.versions.toml` (version catalog). Não inventar versões: se precisar de uma nova, pergunte ou use a mais recente estável e informe qual.
- Não adicionar bibliotecas fora da stack da seção 2 sem justificar e pedir aprovação.

## 10. Testes

- Unitários (JUnit + coroutines-test): `CryptoManager` (round trip, IV diferente a cada chamada), cálculo de distância/bearing, ordenação por distância, serialização JSON de export/import.
- Room: testes instrumentados do DAO com banco em memória.
- ViewModels: testar com repositório fake.
- Toda lógica de `util/geo` e `util/crypto` entrega junto com testes.

## 11. Como trabalhar neste repositório

1. Antes de codar, leia as seções 3 (escopo) e 6 (modelo de dados).
2. Faça mudanças pequenas e focadas, uma camada ou funcionalidade por vez, na ordem: **data → util → ViewModel → UI**.
3. Depois de cada etapa, confirme que o projeto compila (`./gradlew assembleDebug`) e que os testes passam (`./gradlew test`).
4. Se algo estiver ambíguo ou faltar decisão (ex.: nome do pacote, tema visual), pergunte em vez de assumir.
5. Não refatorar nem apagar código fora do que a tarefa pede.
6. Ao terminar, resuma o que foi criado/alterado e o que ficou pendente.

## 12. Ordem sugerida de implementação

1. Configuração: Hilt, Room, KSP, version catalog, `Application` e `MainActivity`.
2. `data/`: entidade, DAO, `AppDatabase`, `WifiRepository` (sem senha ainda).
3. `util/crypto`: `CryptoManager` + integração no Repository.
4. `util/location` e `util/geo`: provider de localização, distância e bearing.
5. UI de lista (com ordenação por distância e destaque da mais próxima).
6. UI de cadastro/edição (GPS atual ou coordenadas digitadas, campo de senha).
7. Tela de seta e distância em tempo real.
8. Export/import JSON.
9. Ajustes de permissões, backup e polimento.