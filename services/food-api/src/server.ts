import { buildApp } from "./app.js";
import { readConfig } from "./config.js";

const config = readConfig();
const app = buildApp(config);

app.log.info(
  {
    host: config.HOST,
    port: config.PORT,
    geminiModel: config.GEMINI_MODEL,
    geminiFallbackModels: config.GEMINI_FALLBACK_MODELS
  },
  "Starting food-api"
);

app.listen({
  host: config.HOST,
  port: config.PORT
}).catch((error) => {
  app.log.error(error);
  process.exit(1);
});
