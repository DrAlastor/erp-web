import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { join } from 'node:path';

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

/**
 * Example Express Rest API endpoints can be defined here.
 * Uncomment and define endpoints as necessary.
 *
 * Example:
 * ```ts
 * app.get('/api/{*splat}', (req, res) => {
 *   // Handle API request
 * });
 * ```
 */

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Proxy inverso para peticiones de API hacia el backend
 */
app.use('/api', async (req, res) => {
  const backendUrl = process.env['BACKEND_URL'] || 'http://backend:8080';
  const targetUrl = `${backendUrl}${req.originalUrl}`;
  try {
    const skipHeaders = ['host', 'expect', 'connection', 'content-length'];
    const headers = new Headers();
    for (const [k, v] of Object.entries(req.headers)) {
      if (!skipHeaders.includes(k.toLowerCase()) && v) {
        if (Array.isArray(v)) v.forEach((item) => headers.append(k, item));
        else headers.set(k, v);
      }
    }

    const isBody = req.method !== 'GET' && req.method !== 'HEAD';
    let bodyData: Buffer | undefined = undefined;
    if (isBody) {
      const chunks: Buffer[] = [];
      for await (const chunk of req) {
        chunks.push(chunk as Buffer);
      }
      bodyData = Buffer.concat(chunks);
    }

    const response = await fetch(targetUrl, {
      method: req.method,
      headers,
      body: bodyData as any,
    });

    res.status(response.status);
    response.headers.forEach((v, k) => {
      if (k.toLowerCase() !== 'transfer-encoding') {
        res.setHeader(k, v);
      }
    });
    const data = await response.arrayBuffer();
    res.send(Buffer.from(data));
  } catch (err) {
    console.error('API proxy error:', err);
    res.status(502).json({ error: 'BAD_GATEWAY', message: 'Error comunicando con el backend' });
  }
});

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point, or it is ran via PM2.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url) || process.env['pm_id']) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
