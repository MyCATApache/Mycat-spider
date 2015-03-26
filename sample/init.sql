create table AUTO_BRAND_1 (
   id integer auto_increment not null,
   NAME varchar(255) not null,
   URL  varchar(255) not null,
   FETCHED_AT datetime not null,
   constraint pk_AUTO_BRAND primary key (id)
);