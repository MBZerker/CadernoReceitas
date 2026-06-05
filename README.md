# Caderno de Receitas

Aplicativo Android/.NET MAUI para restaurantes cadastrarem receitas e treinarem equipes com quiz, modo prova e historico de pontuacao.

## Download

Quando o GitHub Pages estiver ativo, a pagina publica ficara em:

https://mbzerker.github.io/CadernoReceitas/

APK atual:

https://mbzerker.github.io/CadernoReceitas/CadernoReceitas.apk

## Atualizacao

O app consulta o manifesto:

https://raw.githubusercontent.com/MBZerker/CadernoReceitas/main/docs/update.json

Se `versionCode` for maior que a versao instalada, ele avisa o usuario e abre a pagina oficial de download.

## Primeira versao

- Offline, sem login, sem servidor e sem API.
- SQLite local.
- MVVM com CommunityToolkit.Mvvm.
- Shell Navigation.
- CRUD de restaurantes, pracas, pratos, receitas e ingredientes.
- Quiz, modo prova e historico de pontuacao.
- Pagina de download em `docs/`.
