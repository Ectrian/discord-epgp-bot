SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS transport_users;
DROP TABLE IF EXISTS epgp_log;
DROP TABLE IF EXISTS loot;
DROP TABLE IF EXISTS characters;
DROP TABLE IF EXISTS players;
DROP TABLE IF EXISTS loot_alias;
DROP TABLE IF EXISTS loot_game_info;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE players (
  id int(64) unsigned not null auto_increment,
  name varchar(255) not null,
  ep_earned int(64) unsigned not null default 0,
  gp_earned int(64) unsigned not null default 0,
  ep_net int(64) unsigned not null default 0,
  gp_net int(64) unsigned not null default 0,
  notes mediumtext default null,
  PRIMARY KEY(id),
  UNIQUE(name)
);

CREATE TABLE transport_users (
  id varchar(255) not null,
  player_id int(64) unsigned not null,
  name varchar(255) not null,
  PRIMARY KEY(id)
);

CREATE TABLE characters (
  id int(64) unsigned not null auto_increment,
  player_id int(64) unsigned default null,
  name varchar(255) not null,
  level int(8) unsigned default null,
  race varchar(255) default null,
  class varchar(255) default null,
  guild_name varchar(255) default null,
  guild_rank varchar(255) default null,
  talent_spec1_tree varchar(255) default null,
  talent_spec1_tree1 int(8) unsigned not null default 0,
  talent_spec1_tree2 int(8) unsigned not null default 0,
  talent_spec1_tree3 int(8) unsigned not null default 0,
  talent_spec2_tree varchar(255) default null,
  talent_spec2_tree1 int(8) unsigned not null default 0,
  talent_spec2_tree2 int(8) unsigned not null default 0,
  talent_spec2_tree3 int(8) unsigned not null default 0,
  deleted int(1) unsigned not null default 0,
  last_armory_sync int(64) unsigned not null default 0,
  notes mediumtext default null,
  PRIMARY KEY(id),
  FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE SET NULL,
  UNIQUE(name)
);

CREATE TABLE loot (
  id int(64) unsigned not null auto_increment,
  name varchar(255) not null,
  PRIMARY KEY(id),
  UNIQUE(name)
);

CREATE TABLE loot_alias (
  id int(64) unsigned not null auto_increment,
  name varchar(255) not null,
  loot_id int(64) unsigned default null,
  FOREIGN KEY(loot_id) REFERENCES loot(id) ON DELETE RESTRICT,
  PRIMARY KEY(id),
  UNIQUE(name)
);

CREATE TABLE loot_game_info (
  loot_id int(64) unsigned NOT NULL,
  game_id int(64) unsigned NOT NULL,
  game_rarity int(8) unsigned NOT NULL,
  game_slot int(8) unsigned NOT NULL,
  FOREIGN KEY(loot_id) REFERENCES loot(id) ON DELETE RESTRICT,
  UNIQUE(game_id)
);

/* To import loot from an open source database (such as Mangos/Trinity): */
/*
INSERT INTO loot (SELECT DISTINCT name FROM item_template);
INSERT INTO loot_game_info (
	SELECT l.id AS loot_id, it.entry AS game_id, it.quality AS game_rarity 
	FROM item_template AS it
	JOIN loot AS l ON l.name = it.name
);
*/

# type = enum {IMPORT, MERGE, PENALTY, LOOT, STANDBY, RAID, INCENTIVE, DECAY}
CREATE TABLE epgp_log (
  id int(64) unsigned not null auto_increment,
  timestamp int(64) unsigned not null,
  action_timestamp int(64) unsigned not null,
  target_player_id int(64) unsigned not null,
  target_character_id int(64) unsigned default null,
  source_player_id int(64) unsigned not null,
  type int(8) not null,
  raid_type int(16) default null,
  loot_id int(64) unsigned default null,
  ep_delta int(64) not null default 0,
  gp_delta int(64) not null default 0,
  note text default null,
  undoes int(64) unsigned default null,
  undone_by int(64) unsigned default null,
  FOREIGN KEY(target_character_id) REFERENCES characters(id) ON DELETE SET NULL,
  FOREIGN KEY(target_player_id) REFERENCES players(id) ON DELETE RESTRICT,
  FOREIGN KEY(source_player_id) REFERENCES players(id) ON DELETE RESTRICT,
  FOREIGN KEY(loot_id) REFERENCES loot(id) ON DELETE RESTRICT,
  FOREIGN KEY(undoes) REFERENCES epgp_log(id) ON DELETE SET NULL,
  FOREIGN KEY(undone_by) REFERENCES epgp_log(id) ON DELETE SET NULL,
  PRIMARY KEY(id)
);

/*
MIGRATION:

ALTER TABLE epgp_log ADD undoes int(64) unsigned default null;
ALTER TABLE epgp_log ADD undone_by int(64) unsigned default null;
ALTER TABLE epgp_log ADD CONSTRAINT fk_undoes FOREIGN KEY (undoes) REFERENCES epgp_log(id) ON DELETE SET NULL;
ALTER TABLE epgp_log ADD CONSTRAINT fk_undone_by FOREIGN KEY (undone_by) REFERENCES epgp_log(id) ON DELETE SET NULL;
ALTER TABLE epgp_log ADD action_timestamp int(64) unsigned not null;
UPDATE epgp_log SET action_timestamp = timestamp;
ALTER TABLE loot_game_info ADD game_slot int(8) unsigned NOT NULL;
*/