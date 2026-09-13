# ===================================================================
# Etapa 1: Builder (Compilación de Angular 22 SSR para Producción)
# ===================================================================
FROM node:24-alpine AS builder
WORKDIR /app

# Instalar dependencias usando package-lock.json para builds reproducibles
COPY package*.json ./
RUN npm ci

# Copiar código fuente y compilar proyecto
COPY . .
RUN npm run build

# ===================================================================
# Etapa 2: Runtime (Servidor Express SSR de Producción)
# ===================================================================
FROM node:24-alpine AS runner
WORKDIR /app

# Copiar manifiestos e instalar únicamente dependencias de producción
COPY package*.json ./
RUN npm ci --omit=dev && npm cache clean --force

# Copiar la aplicación compilada (browser y server)
COPY --from=builder /app/dist/frontend ./dist/frontend

# Asignar permisos al usuario no privilegiado 'node'
RUN chown -R node:node /app
USER node

# Configurar variables de entorno y puerto de escucha
ENV PORT=4000
ENV NODE_ENV=production

EXPOSE 4000

# Iniciar servidor Node.js
CMD ["node", "dist/frontend/server/server.mjs"]
