FROM node:20-bookworm-slim

WORKDIR /app

RUN corepack enable

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY data data
COPY packages/food-contracts/package.json packages/food-contracts/package.json
COPY packages/food-contracts/tsconfig.json packages/food-contracts/tsconfig.json
COPY packages/food-contracts/src packages/food-contracts/src
COPY services/food-api/package.json services/food-api/package.json
COPY services/food-api/tsconfig.json services/food-api/tsconfig.json
COPY services/food-api/src services/food-api/src

RUN corepack pnpm install --frozen-lockfile

EXPOSE 3000

CMD ["./services/food-api/node_modules/.bin/tsx", "services/food-api/src/bootstrap.ts"]
