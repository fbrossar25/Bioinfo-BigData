create schema PUBLIC
;

create table HIERARCHY
(
	H_ID BIGINT
		primary key,
	H_GROUP VARCHAR(255) not null,
	H_KINGDOM VARCHAR(255) not null,
	H_ORGANISM VARCHAR(255) not null
		unique,
	H_SUBGROUP VARCHAR(255) not null
)
;

create table REPLICONS
(
	R_ID BIGINT
		primary key,
	R_COMPUTED BOOLEAN,
	R_DINUCLEOTIDES VARCHAR(16777216),
	R_DOWNLOADED BOOLEAN,
	R_REPLICON VARCHAR(255) not null
		unique,
	R_TRINUCLEOTIDES VARCHAR(16777216),
	R_VERSION INTEGER,
	R_HIERARCHY BIGINT not null
		constraint FK_HIERARCHY
			references HIERARCHY
				on delete cascade
)
;

