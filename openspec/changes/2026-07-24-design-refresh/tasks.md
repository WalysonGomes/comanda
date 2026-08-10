## 1. Preservação e decisões

- [x] 1.1 Auditar o worktree existente e criar checkpoint recuperável do código, assets e build estático, excluindo ruído de permissões
- [x] 1.2 Formalizar proposal, design, tarefas e deltas na change existente `2026-07-24-design-refresh`
- [x] 1.3 Revisar as referências locais `.design`, selecionar logo/screenshots aprovados, preservar somente assets derivados aprovados em caminhos versionados da aplicação e confirmar que `.design` permanece não rastreada
- [x] 1.4 Escolher e registrar o conjunto autoritativo de tokens

## 2. Roteamento e landing

- [x] 2.1 Implementar a matriz de detecção de domínio raiz, aliases, localhost/previews e subdomínios de tenant sem usar o frontend como fronteira de segurança
- [x] 2.2 Adicionar testes de roteamento para domínio raiz, tenant válido, hosts reservados, IP/localhost e escape de desenvolvimento
- [x] 2.3 Concluir a responsividade da landing em larguras móveis, tablet e desktop, sem overflow ou conteúdo inacessível
- [x] 2.4 Ligar e testar cada CTA, link de seção, onboarding, login, termos, privacidade e contato; remover destinos fictícios
- [x] 2.5 Corrigir a copy: qualificar polling, remover notificação automática e impedir alegações de persistência sem sucesso confirmado
- [x] 2.6 Adicionar testes de renderização, conteúdo crítico e interação da landing

## 3. Design system e PWA

- [x] 3.1 Sincronizar os tokens aprovados em `index.css`, specs do design system e demais fontes versionadas, sem editar ou versionar `.design`
- [x] 3.2 Sincronizar `theme_color`, tags runtime, manifest, ícones e assets de marca relevantes
- [x] 3.3 Verificar consistência de cores e rótulos de status entre lista, detalhe e stepper
- [x] 3.4 Verificar contraste, landmarks, headings, nomes acessíveis, navegação por teclado, foco visível e textos alternativos
- [x] 3.5 Verificar `prefers-reduced-motion` e garantir conteúdo visível sem animações

## 4. Verificação e aceitação

- [x] 4.1 Executar build, lint e testes do frontend
- [x] 4.2 Executar testes relevantes do backend e do serving estático, incluindo referências a assets, fallback SPA, manifest e Service Worker
- [x] 4.3 Comparar visualmente landing e painel contra as referências aprovadas em viewport móvel e desktop
- [x] 4.4 Validar que nenhum texto contradiz o bloqueador conhecido de checkout e registrar a decisão de release
- [x] 4.5 Validar esta change e todas as specs com OpenSpec em modo estrito
- [ ] 4.6 Ao arquivar a change, sincronizar os deltas com as specs consolidadas e confirmar `openspec validate --all --strict`
