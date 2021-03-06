
drop table live_posts_by_day;
drop table live_posts_experts_by_hour;
drop table live_tag_counts;
#cassandra tables
drop table tag_counts;
drop table tag_counts_by_month;
drop table faq_answered;
drop table faq_unanswered;
drop table tag_experts;

#http://www.datastax.com/dev/blog/we-shall-have-order

CREATE TABLE tag_counts (rundate timestamp, id text, name text, count int,
    PRIMARY KEY ((rundate), count, name)) WITH CLUSTERING ORDER BY (count DESC, name DESC);

create table tag_counts_by_month (year int, month int, name text, count int,
    PRIMARY KEY ((year, month), count, name)) WITH CLUSTERING ORDER BY (count DESC, name DESC);


CREATE TABLE faq_answered (id text, tags text, creation_date text, cdate timestamp, title text, view_count int, pop_count int,
   PRIMARY KEY ((tags), view_count, id) ) WITH CLUSTERING ORDER BY (view_count DESC, id ASC);
CREATE TABLE faq_unanswered (id text, tags text, creation_date text, cdate timestamp, title text, view_count int, pop_count int,
    PRIMARY KEY ((tags), view_count, id) ) WITH CLUSTERING ORDER BY (view_count DESC, id ASC);

CREATE TABLE tag_experts (tag text, expert_id text, expert_name text, ans_count int,
    PRIMARY KEY ((tag), ans_count, expert_name)) WITH CLUSTERING ORDER BY (ans_count DESC, expert_name DESC);



CREATE TABLE live_posts_by_day (group_day text, id text, creation_date timestamp, post_type_id text, accepted_answer_id text, parent_id text, title text, tags text,
    PRIMARY KEY ((group_day), creation_date, id)) WITH CLUSTERING ORDER BY (creation_date DESC, id DESC);

CREATE TABLE live_posts_experts_by_hour (group_hour text, id text, creation_date timestamp, title text, tags text, experts text,
    PRIMARY KEY ((group_hour), creation_date, id)) WITH CLUSTERING ORDER BY (creation_date DESC, id DESC);

create table posts_data (table_name text, group_val text,
  primary key ((table_name), group_val)) with clustering order by (group_val desc);


CREATE TABLE live_tag_counts (rundate timestamp, id text, name text, count int,
    PRIMARY KEY ((rundate), count, name)) WITH CLUSTERING ORDER BY (count DESC, name DESC);
