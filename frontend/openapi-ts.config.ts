import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "../backend/src/main/resources/openapi/vocavista-api.yaml",
  output: "src/api/generated",
  plugins: ["@hey-api/client-fetch", "@hey-api/sdk", "@hey-api/typescript"]
});
