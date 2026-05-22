create docker network "comic-network" if haven't already:
`docker network create comic-network`

build then run:
`docker compose build`
`docker compose up -d`

if `docker compose up -d` fail first time with comic_db error, run it again.

insert mock data:
`docker exec -it comic_db psql -U postgres -d comic -f /insert-data.sql`