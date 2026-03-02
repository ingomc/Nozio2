import { buildApp } from "./app.js";
import { readConfig } from "./config.js";

const config = readConfig();
const app = buildApp(config);

app.listen({
  host: config.HOST,
  port: config.PORT
}).catch((error) => {
  app.log.error(error);
  process.exit(1);
});
