CREATE TABLE IF NOT EXISTS app_user (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(32)  NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS chapter (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL CHECK (length(trim(name)) > 0),
    parent_legion VARCHAR(255),
    marines_count INTEGER      NOT NULL CHECK (marines_count > 0 AND marines_count <= 1000),
    world         VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS space_marine (
    id            BIGSERIAL PRIMARY KEY CHECK (id > 0),
    name          VARCHAR(255)     NOT NULL CHECK (length(trim(name)) > 0),
    coord_x       DOUBLE PRECISION NOT NULL CHECK (coord_x > -634),
    coord_y       REAL             NOT NULL CHECK (coord_y > -126),
    creation_date TIMESTAMP        NOT NULL DEFAULT now(),
    chapter_id    BIGINT           NOT NULL REFERENCES chapter (id) ON DELETE RESTRICT,
    health        BIGINT           NOT NULL CHECK (health > 0),
    height        INTEGER          NOT NULL,
    category      VARCHAR(20)      NOT NULL CHECK (category IN ('SCOUT', 'AGGRESSOR', 'TACTICAL', 'LIBRARIAN', 'APOTHECARY')),
    weapon_type   VARCHAR(20)      NOT NULL CHECK (weapon_type IN ('HEAVY_BOLTGUN', 'MELTAGUN', 'COMBI_PLASMA_GUN', 'FLAMER', 'HEAVY_FLAMER'))
);

CREATE INDEX IF NOT EXISTS idx_space_marine_chapter ON space_marine (chapter_id);
