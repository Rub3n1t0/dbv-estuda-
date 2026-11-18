Membros do Grupo (Kelly Dantas, Rubens, Iuji, Wesley)

# DBV Estuda+ (Android · Kotlin)

App de perguntas e respostas para Desbravadores: treinar especialidades, aplicar provas e acompanhar o desempenho. Visual baseado no protótipo [Prot_DBV](https://well-2003.github.io/Prot_DBV/) (azul `#0E2A47` e amarelo `#F5B700`).

## Tecnologias

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** (SQLite): os dados ficam salvos no aparelho, inclusive sem internet
- Navigation Compose, ViewModel, Coroutines/Flow
- `minSdk 24` (Android 7.0) · `targetSdk 35`

## Como abrir

1. Abra a pasta no **Android Studio** (Ladybug ou mais novo) em *File › Open*.
2. Espere o Gradle sincronizar (o Android Studio baixa o Gradle 8.14.3 e as dependências).
   - Se pedir o *Gradle wrapper*, aceite, ou rode `gradle wrapper` uma vez na pasta.
3. Rode no emulador ou no celular (▶ Run).

O Android Studio pode sugerir atualizar AGP, Kotlin e bibliotecas. Pode aceitar.

## Contas de demonstração

| Papel | Nome | Senha |
|---|---|---|
| Desbravadora | Ana Souza | `aguias2026` |
| Instrutor | Carlos Lima | `instrutor26` |
| Desbravadores | Pedro Henrique, Marina Costa, Júlia Santos | `dbv2026` |

Na primeira abertura o app carrega 14 especialidades com 131 perguntas, cria essas contas, uma prova aberta de Primeiros socorros e um histórico de tentativas para os gráficos aparecerem com dados.

## O que o app faz

**Desbravador**
- *Início*: provas abertas do clube, média geral e últimas atividades.
- *Especialidades*: catálogo com busca e filtro por área, link para os requisitos oficiais no MDA Wiki.
- *Treino*: escolhe 5, 10, 20 ou todas as perguntas; mostra certo/errado e a explicação depois de cada uma.
- *Prova*: perguntas fixas definidas pelo instrutor, alternativas embaralhadas, tempo limite opcional, uma entrega por pessoa.
- *Resultado*: nota de 0 a 10, aprovado/reprovado e revisão de todas as respostas.
- *Desempenho*: média, % de acertos, evolução das notas, média por área e melhor nota por especialidade.

**Instrutor**
- *Provas*: cria prova (especialidade, nº de perguntas, nota mínima, tempo), abre/fecha, vê notas da turma e % de acerto de cada pergunta (para ver o que a turma mais errou).
- *Turma*: lista dos desbravadores do clube com média e provas aprovadas; toque para ver o desempenho individual.
- *Perguntas*: banco de cada especialidade (criar, editar, excluir), nova especialidade, **importar** e **exportar JSON**.

## Banco de dados

```
usuarios ─┐
          └─< tentativas ─< respostas >─ perguntas >─ especialidades
provas ─< prova_perguntas >─ perguntas          provas >─ especialidades
```

- Perguntas excluídas ficam inativas, e uma pergunta já respondida que é editada vira uma nova versão. Assim o histórico e as revisões continuam corretos.
- A senha é guardada como hash SHA-256.
- Arquivo: `dbv_estuda.db` (ver em *App Inspection › Database Inspector* no Android Studio).

## Adicionar perguntas por JSON

Use a aba **Perguntas › Importar JSON** do instrutor. Mesmo formato de `app/src/main/assets/perguntas.json`:

```json
{
  "especialidades": [
    {
      "codigo": "AR-024",
      "nome": "Vida silvestre",
      "area": "AR",
      "nivel": 2,
      "descricao": "Opcional",
      "perguntas": [
        {
          "enunciado": "Texto da pergunta?",
          "opcoes": ["A", "B", "C", "D"],
          "correta": 1,
          "explicacao": "Aparece no treino e na revisão."
        }
      ]
    }
  ]
}
```

- `correta` é o índice da alternativa certa, começando em **0**.
- Aceita de 2 a 6 alternativas.
- Perguntas com o mesmo enunciado na mesma especialidade são ignoradas, então dá para importar o mesmo arquivo de novo sem duplicar.
- Áreas: `ID` Ideais, `AD` ADRA, `HM` Artes e Habilidades Manuais, `AA` Agrícolas, `AM` Missionárias, `AM-EB` Estudos Bíblicos, `AP` Profissionais, `AR` Recreativas, `CS` Ciência e Saúde, `EN` Estudo da Natureza, `HD` Domésticas, `ME` Mestrados.

As perguntas iniciais foram escritas a partir dos temas de cada especialidade. Vale um instrutor revisar e completar com os requisitos oficiais do [MDA Wiki](https://mda.wiki.br/).

## Estrutura

```
app/src/main/java/com/dbv/estuda/
├── DbvApp.kt              # Application: banco, repositório, sessão e carga inicial
├── MainActivity.kt        # Navegação entre telas
├── data/
│   ├── Entidades.kt       # Tabelas Room e resultados de consultas
│   ├── Daos.kt            # Consultas (inclui relatórios de desempenho)
│   ├── AppDatabase.kt
│   ├── Repositorio.kt     # Login, provas, notas, importar/exportar, dados demo
│   └── Sessao.kt          # Usuário logado (SharedPreferences)
└── ui/
    ├── theme/Tema.kt
    ├── comum/Componentes.kt   # Insígnia, cartões, gráficos
    └── telas/                 # Acesso, Desbravador, Especialidade, Quiz, Instrutor
```

## Próximos passos possíveis

- Sincronizar entre aparelhos (Firebase/Firestore ou um backend próprio): hoje cada aparelho tem seu próprio banco. Exportar/importar JSON serve para compartilhar o banco de perguntas.
- Lições de estudo por especialidade e as seis etapas do protótipo (planejamento → investidura).
- Papéis de secretaria e diretoria do protótipo.
