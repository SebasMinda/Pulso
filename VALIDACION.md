# Validación Pulso 0.12.1

Las canciones nuevas usan la primera opción de AudioChoice.available, ordenada por tasa de audio de mayor a menor. Se aplica tanto al listado como a la descarga individual y a los enlaces. El diálogo manual aparece solamente al editar Calidad en una entrada del listado. La descarga individual conserva el listado.

Pruebas unitarias, Lint y compilaciones de desarrollo y producción completadas. Ejecución: https://github.com/SebasMinda/Pulso/actions/runs/38198624233. La prueba AudioChoiceTest.displaysHighestBitrateFirstWithoutDuplicateFormats confirma el orden usado por la selección automática.

APK firmado con la clave estable y certificado verificado. Validación visual y descarga real en un teléfono pendientes.
