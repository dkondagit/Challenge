Drop Table if exists Users cascade;
Drop Table if exists UserProfile cascade;
Drop Table if exists authorities cascade;
Drop Table if exists userconnection cascade;

create table UserConnection (
  userId varchar(255) not null,
  providerId varchar(255) not null,
  providerUserId varchar(255),
  rank int not null,
  displayName varchar(255),
  profileUrl varchar(512),
  imageUrl varchar(512),
  accessToken varchar(1024) not null,
  secret varchar(255),
  refreshToken varchar(255),
  expireTime bigint,
  primary key (userId, providerId, providerUserId));
create unique index UserConnectionRank on UserConnection(userId, providerId, rank);

create table UserProfile (
  userId varchar(255) not null,
  email varchar(255),
  firstName varchar(255),
  lastName varchar(255),
  city varchar(255),
  name  varchar(255),
  username varchar(255),
  userEntityId int,
  constraint fk_userprofile_entities foreign key(userEntityId) references entities(entity_id),
  primary key (userId));
create unique index UserProfilePK on UserProfile(userId);

create table users(
      username varchar(50) not null primary key,
      password varchar(50) not null,
      enabled boolean not null);

create table authorities (
    username varchar(50) not null,
    authority varchar(50) not null,
    constraint fk_authorities_users foreign key(username) references users(username));
    create unique index ix_auth_username on authorities (username,authority);

    
    
    
    
    
    
-- Table: public.relationship2
-- remove it outside

-- DROP TABLE public.relationship2;
DROP TABLE public.relationship2;
    
CREATE TABLE public.relationship2
(
  entity_id1 integer NOT NULL,
  entity_id2 integer NOT NULL,
  attribute_id integer NOT NULL,
  CONSTRAINT relationship2_pkey PRIMARY KEY (entity_id1, entity_id2, attribute_id),
  CONSTRAINT fk_1mujrn3bp408pujeeg4skmwaw FOREIGN KEY (entity_id1)
      REFERENCES public.entities (entity_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_7ml7ux1mvkqa295fvh26epds1 FOREIGN KEY (entity_id2)
      REFERENCES public.entities (entity_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.relationship2
  OWNER TO postgres;
  

create sequence serial start 1;
