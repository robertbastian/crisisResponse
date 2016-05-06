--
-- PostgreSQL database dump
--

-- Dumped from database version 9.4.5
-- Dumped by pg_dump version 9.5.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: Collection; Type: TABLE; Schema: public; Owner: root
--

CREATE TABLE "Collection" (
    id smallint NOT NULL,
    name character varying(250) DEFAULT ''::character varying NOT NULL,
    status integer NOT NULL
);


ALTER TABLE "Collection" OWNER TO root;

--
-- Name: Collection_id_seq; Type: SEQUENCE; Schema: public; Owner: root
--

CREATE SEQUENCE "Collection_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "Collection_id_seq" OWNER TO root;

--
-- Name: Collection_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: root
--

ALTER SEQUENCE "Collection_id_seq" OWNED BY "Collection".id;


--
-- Name: Interactions; Type: TABLE; Schema: public; Owner: root
--

CREATE TABLE "Interactions" (
    "from" character varying(255) NOT NULL,
    "to" character varying(255) NOT NULL,
    tweet integer NOT NULL
);


ALTER TABLE "Interactions" OWNER TO root;

--
-- Name: Tweet; Type: TABLE; Schema: public; Owner: root
--

CREATE TABLE "Tweet" (
    id integer NOT NULL,
    author character varying NOT NULL,
    text character varying NOT NULL,
    "time" timestamp without time zone NOT NULL,
    collection integer NOT NULL,
    latitude real,
    longitude real,
    sentiment real,
    recency real,
    corroboration real,
    proximity real
);


ALTER TABLE "Tweet" OWNER TO root;

--
-- Name: Tweet_id_seq; Type: SEQUENCE; Schema: public; Owner: root
--

CREATE SEQUENCE "Tweet_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "Tweet_id_seq" OWNER TO root;

--
-- Name: Tweet_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: root
--

ALTER SEQUENCE "Tweet_id_seq" OWNED BY "Tweet".id;


--
-- Name: User; Type: TABLE; Schema: public; Owner: root
--

CREATE TABLE "User" (
    name character varying(50) NOT NULL,
    competence real,
    popularity real,
    latitude real,
    longitude real
);


ALTER TABLE "User" OWNER TO root;

--
-- Name: Words; Type: TABLE; Schema: public; Owner: root
--

CREATE TABLE "Words" (
    word character varying NOT NULL,
    type character varying(12) NOT NULL,
    sentiment integer NOT NULL,
    tweet integer NOT NULL
);


ALTER TABLE "Words" OWNER TO root;

--
-- Name: id; Type: DEFAULT; Schema: public; Owner: root
--

ALTER TABLE ONLY "Collection" ALTER COLUMN id SET DEFAULT nextval('"Collection_id_seq"'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: root
--

ALTER TABLE ONLY "Tweet" ALTER COLUMN id SET DEFAULT nextval('"Tweet_id_seq"'::regclass);


--
-- Data for Name: Collection; Type: TABLE DATA; Schema: public; Owner: root
--

COPY "Collection" (id, name, status) FROM stdin;
\.


--
-- Name: Collection_id_seq; Type: SEQUENCE SET; Schema: public; Owner: root
--

SELECT pg_catalog.setval('"Collection_id_seq"', 1, false);


--
-- Data for Name: Interactions; Type: TABLE DATA; Schema: public; Owner: root
--

COPY "Interactions" ("from", "to", tweet) FROM stdin;
\.


--
-- Data for Name: Tweet; Type: TABLE DATA; Schema: public; Owner: root
--

COPY "Tweet" (id, author, text, "time", collection, latitude, longitude, sentiment, recency, corroboration, proximity) FROM stdin;
\.


--
-- Name: Tweet_id_seq; Type: SEQUENCE SET; Schema: public; Owner: root
--

SELECT pg_catalog.setval('"Tweet_id_seq"', 1, false);


--
-- Data for Name: User; Type: TABLE DATA; Schema: public; Owner: root
--

COPY "User" (name, competence, popularity, latitude, longitude) FROM stdin;
\.


--
-- Data for Name: Words; Type: TABLE DATA; Schema: public; Owner: root
--

COPY "Words" (word, type, sentiment, tweet) FROM stdin;
\.


--
-- Name: Collection_pkey; Type: CONSTRAINT; Schema: public; Owner: root
--

ALTER TABLE ONLY "Collection"
    ADD CONSTRAINT "Collection_pkey" PRIMARY KEY (id);


--
-- Name: Tweet_pkey; Type: CONSTRAINT; Schema: public; Owner: root
--

ALTER TABLE ONLY "Tweet"
    ADD CONSTRAINT "Tweet_pkey" PRIMARY KEY (id);


--
-- Name: User_pkey; Type: CONSTRAINT; Schema: public; Owner: root
--

ALTER TABLE ONLY "User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (name);


--
-- Name: Interactions_tweet_fkey; Type: FK CONSTRAINT; Schema: public; Owner: root
--

ALTER TABLE ONLY "Interactions"
    ADD CONSTRAINT "Interactions_tweet_fkey" FOREIGN KEY (tweet) REFERENCES "Tweet"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: collection; Type: FK CONSTRAINT; Schema: public; Owner: root
--

ALTER TABLE ONLY "Tweet"
    ADD CONSTRAINT collection FOREIGN KEY (collection) REFERENCES "Collection"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: tweet; Type: FK CONSTRAINT; Schema: public; Owner: root
--

ALTER TABLE ONLY "Words"
    ADD CONSTRAINT tweet FOREIGN KEY (tweet) REFERENCES "Tweet"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: public; Type: ACL; Schema: -; Owner: root
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM root;
GRANT ALL ON SCHEMA public TO root;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

