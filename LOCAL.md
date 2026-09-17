# Ejecutar el ERP en localhost

Entorno preparado en esta maquina: PostgreSQL 18.6 en el puerto 5433, backend en 8080 y frontend en 4200. Hay un unico backend Spring Boot para todos los modulos y casos de uso implementados del ERP. Los comandos npm son atajos para arrancarlo con Gradle; no convierten el backend a Node.js.

La base local conserva el nombre inicial `erp_cu01_local`, pero no esta restringida a CU-01: las migraciones futuras pueden agregar las tablas de todos los modulos. Actualmente contiene las migraciones existentes del repositorio; no es una copia del esquema completo de AWS. En esta rama la funcionalidad implementada es autenticacion; arrancar el servidor no implementa automaticamente los casos de uso pendientes.

La configuracion privada esta en `backend/.env.local` (ignorada por Git). Spring Boot la carga mediante el perfil `local`; `.env.example` sigue siendo una referencia de AWS.

## Arranque

PostgreSQL debe estar arrancado antes de iniciar el backend. Si la instancia local esta detenida, ejecutar:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe' start -D E:\SI2\.local-postgres-cu01\data -l E:\SI2\.local-postgres-cu01\postgres.log -o '-p 5433 -h 127.0.0.1' -w
```

Desde la raiz del repositorio, en una terminal para el backend:

```powershell
cd E:\SI2\erp-web
npm.cmd run start:dev
```

El comando ejecuta Gradle con el perfil `local`, carga `backend/.env.local` y arranca el backend completo. Se eliminaron los scripts PowerShell auxiliares. No iniciar otra instancia si el puerto 8080 ya esta ocupado.

En otra terminal para el frontend:

```powershell
cd E:\SI2\erp-web
npm.cmd run dev
```

Tambien funcionan `npm.cmd run start:dev` desde `backend` y `npm.cmd run dev` desde `frontend`. No hay que ejecutar `npm install` para los atajos de raiz/backend, ya que no agregan dependencias. El frontend necesita sus dependencias habituales, ya instaladas en esta maquina.

Se usa `npm.cmd` porque PowerShell bloquea `npm.ps1` en esta maquina. En terminales donde `npm` funciona se pueden usar `npm run start:dev` y `npm run dev`.

Abrir http://localhost:4200/login y entrar con `admin@erp.com` / `Admin123!`. Health del backend: http://localhost:8080/api/health.

Si los servicios ya estan arrancados, no repetir los comandos: sus puertos estaran ocupados. Backend y frontend se detienen con Ctrl+C en sus terminales.

Para detener exclusivamente la instancia local de prueba de PostgreSQL:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe' stop -D E:\SI2\.local-postgres-cu01\data -m fast
```

## Alcance de la prueba

Se comprobaron login, refresh, logout, rechazo del refresh tras logout y CORS para `http://localhost:4200`. Tambien se verificaron contra PostgreSQL local la persistencia de tres intentos fallidos, el bloqueo y el reinicio del contador al vencerlo; el usuario temporal se elimino despues. El build completo del backend, incluida la prueba de arranque, paso. No se desplego ni se hizo push.

Flyway advierte que PostgreSQL 18 es mas nuevo que la version 16 que soporta oficialmente esta version del proyecto; las migraciones locales pasaron. Antes de desplegar, hay que repetir la validacion con la version y el esquema reales de RDS. El certificado RDS no se necesita para esta conexion local.
