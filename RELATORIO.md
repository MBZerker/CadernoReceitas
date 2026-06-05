# Relatorio de Implementacao

## Resumo

Foi criada a primeira versao navegavel do aplicativo Caderno de Receitas, partindo do template MAUI existente.

## Arquitetura

- `Models`: entidades do dominio e manifesto de update.
- `Data`: banco SQLite local.
- `Services`: quiz e verificacao de atualizacao.
- `ViewModels`: MVVM com CommunityToolkit.Mvvm.
- `Views`: telas XAML do app.
- `docs`: GitHub Pages, APK e `update.json`.

## Funcionalidades Entregues

- Cadastro de restaurantes.
- Cadastro de pracas vinculadas a restaurantes.
- Cadastro de pratos vinculados a pracas.
- Cadastro de receitas vinculadas a pratos.
- Cadastro de ingredientes vinculados a receitas.
- Home com estatisticas e historico recente.
- Quiz de aprendizagem.
- Modo prova.
- Historico de pontuacao.
- Verificacao de update baseada em `docs/update.json`.
- APK Release em `docs/CadernoReceitas.apk`.

## Publicacao

Arquivos preparados para GitHub Pages:

- `docs/index.html`
- `docs/update.json`
- `docs/CadernoReceitas.apk`
- `docs/CadernoReceitas-0.1.0.apk`
- `docs/.nojekyll`

## Validacao

- `dotnet build -f net9.0-android`: sucesso.
- `dotnet publish -f net9.0-android -c Release -p:AndroidPackageFormat=apk`: sucesso.

Aviso observado:

- `XA0141` sobre alinhamento de `libe_sqlite3.so`, vindo do pacote SQLite. Nao bloqueia a compilacao nem a geracao do APK.
