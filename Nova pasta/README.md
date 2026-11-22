# Peugeot 308 Smart Drive

Projeto Android nativo em Java, sem bibliotecas externas, com `minSdk 25` (Android 7.1) e tela horizontal. Inclui launcher HOME, painel, telas de parâmetros, gráficos simples de temperatura/RPM, conexão Bluetooth Classic RFCOMM para ELM327 pareado e leitura OBD-II genérica.

## Gerar APK pelo GitHub
1. Crie um repositório vazio no GitHub.
2. Extraia todo o ZIP e envie **todo o conteúdo**, inclusive a pasta oculta `.github`.
3. Abra **Actions** e aguarde a execução **Gerar APK**.
4. Na execução verde, baixe o Artifact `Peugeot308-SmartDrive-APK`.
5. Extraia o ZIP do artifact. O arquivo é `app-debug.apk`.

O workflow usa o Android SDK já instalado no runner e instala apenas `platform-tools`, `platforms;android-35` e `build-tools;35.0.0`. Não solicita o pacote legado `tools`.

## Compatibilidade
- Android 7.1 ou superior (minSdk 25).
- Compilação com Java 17, Gradle 8.7 e Android Gradle Plugin 8.6.1.
- Tela horizontal.
- Para OBD2: adaptador ELM327 Bluetooth Classic previamente pareado nas configurações Bluetooth do Android. Adaptadores BLE/Wi-Fi não são suportados nesta versão.

## O que está implementado
- Atividade declarada como launcher padrão (HOME) e launcher comum.
- Interface automotiva clara, cartões de dados e navegação inferior.
- Conexão RFCOMM SPP com dispositivos Bluetooth já pareados.
- Inicialização ELM327 (`ATZ`, `ATE0`, `ATL0`, `ATS0`, `ATH0`, `ATSP0`).
- Leitura genérica de velocidade, RPM, temperatura do líquido, temperatura de admissão, MAP, carga, posição da borboleta, STFT, LTFT, MAF e tensão do módulo quando a ECU suportar os PIDs.
- Gráficos locais básicos de temperatura e RPM.
- Leitura de DTC armazenados e pendentes e comando de apagar falhas com confirmação.

## Limitações importantes
Esta é uma primeira versão funcional de base, não um scanner Peugeot específico. A ECU do 308 EC5 Flex pode não expor todos os parâmetros por PIDs OBD-II genéricos. Consumo km/L, autonomia, combustível flex, sonda lambda avançada, DTC permanentes, freeze frame, temperatura externa e nível do tanque ainda não estão implementados como funções completas; o app não inventa valores e mostra N/D quando não recebe um dado. A tela do carro está com um marcador ilustrativo temporário; substitua por uma imagem licenciada/foto do seu 308 para obter o visual final desejado.

A comunicação OBD foi estruturada para ELM327 Bluetooth Classic, mas precisa ser validada no adaptador e na ECU reais. Use com o veículo parado para diagnóstico; não opere a tela enquanto dirige. Apagar DTC pode apagar informações úteis de diagnóstico.

## Personalização da imagem do carro
Adicione sua imagem em `app/src/main/res/drawable/` e ajuste a função `home()` em `MainActivity.java` para exibi-la com um `ImageView` centralizado. Use uma imagem própria ou com licença adequada.
