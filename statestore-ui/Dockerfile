FROM node:13.8.0

ENV CONTAINER_PATH /var/www/docker-vue

WORKDIR $CONTAINER_PATH

COPY package*.json ./

RUN npm install

COPY . .

EXPOSE 8080

CMD ["npm", "run", "serve"]
