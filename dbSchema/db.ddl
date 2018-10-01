create table HIERARCHY
(
	H_ID BIGINT
		constraint CONST_HIERARCHY_PK
			primary key,
	H_ORGANISM VARCHAR(255) not null
		constraint HIERARCHY_H_ORGANISM_UINDEX
			unique,
	H_GROUP VARCHAR(255) not null,
	H_SUBGROUP VARCHAR(255) not null,
	H_KINGDOM VARCHAR(255) not null
)
;

create table REPLICONS
(
	R_ID BIGINT not null
		primary key,
	R_REPLICON VARCHAR(255) not null
		constraint REPLICONS_R_REPLICON_UINDEX
			unique,
	R_VERSION INTEGER default 1 not null,
	R_DINUCLEOTIDES VARCHAR(16777216),
	R_TRINUCLEOTIDES VARCHAR(16777216),
	R_DOWNLOADED BOOLEAN default FALSE not null,
	R_COMPUTED BOOLEAN default FALSE not null,
	R_HIERARCHY BIGINT not null
		constraint FK_REPLICONS_REF_HIERARCHY
			references HIERARCHY
				on update cascade on delete cascade
)
;

