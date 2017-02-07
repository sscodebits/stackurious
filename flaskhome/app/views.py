# jsonify creates a json representation of the response
from flask import jsonify, request
from flask import render_template
from app import app
from datetime import datetime
import urllib2
import json

# importing Cassandra modules from the driver we just installed
from cassandra.cluster import Cluster

# Setting up connections to cassandra

# Change the bolded text to your seed node public dns (no < or > symbols but keep quotations. Be careful to copy quotations as it might copy it as a special character and throw an error. Just delete the quotations and type them in and it should be fine. Also delete this comment line
cluster = Cluster(['ec2-52-33-132-53.us-west-2.compute.amazonaws.com'])

# Change the bolded text to the keyspace which has the table you want to query. Same as above for < or > and quotations. Also delete this comment line
session = cluster.connect('stackoverflow')

@app.route('/')
@app.route('/index')
def index():
  user = { 'nickname': 'Miguel' } # fake user
  return render_template("index.html", title = 'Home', user = user)

@app.route('/api/<email>/<date>')
def get_email(email, date):
       stmt = "SELECT * FROM email WHERE id=%s and date=%s"
       response = session.execute(stmt, parameters=[email, date])
       response_list = []
       for val in response:
            response_list.append(val)
       jsonresponse = [{"first name": x.fname, "last name": x.lname, "id": x.id, "message": x.message, "time": x.time} for x in response_list]
       return jsonify(emails=jsonresponse)

def get_data(stmt, params):
       count = request.args.get('count', '');
       if (count != ''):
          stmt = stmt + ' LIMIT ' + count
       response = session.execute(stmt, parameters=params)
       response_list = []
       for val in response:
             response_list.append(val)
       return response_list;

@app.route('/api/tag/<tag>')
def get_tags(tag):
       stmt = 'SELECT * FROM tag_counts WHERE name=%s'
       response_list = get_data(stmt, [tag])
       jsonresponse = [{"id": x.id, "name": x.name, "count": x.count} for x in response_list]
#       return jsonify(tags=jsonresponse)
       return jsonresponse

@app.route('/api/tag/<year>/<month>')
def get_tags_by_month(year, month):
       stmt = 'SELECT * FROM tag_counts_by_month WHERE year=%s and month=%s'
       response_list = get_data(stmt, [int(year), int(month)])
       jsonresponse = [{"name": x.name, "count": x.count} for x in response_list]
       return jsonify(tags=jsonresponse)


@app.route('/api/tag/ans/<tag>')
def get_ans(tag):
       stmt = 'SELECT * FROM faq_answered WHERE tags=%s'
       response_list = get_data(stmt, [tag])
       jsonresponse = [{"name": x.title, "creation_date": x.creation_date, "count": x.view_count} for x in response_list]
       return jsonify(faq=jsonresponse)

@app.route('/api/tag/experts/<tag>')
def get_experts(tag):
       stmt = 'SELECT * FROM tag_experts WHERE tag=%s'
       response_list = get_data(stmt, [tag])
       jsonresponse = [{"name": x.expert_name, "count": x.ans_count} for x in response_list]
       return jsonify(expert=jsonresponse)

@app.route('/api/tag/uans/<tag>')
def get_uans(tag):
       stmt = 'SELECT * FROM faq_unanswered WHERE tags=%s'
       response_list = get_data(stmt, [tag])
       jsonresponse = [{"name": x.title, "creation_date": x.creation_date, "count": x.view_count} for x in response_list]
       return jsonify(faq=jsonresponse)
       

@app.route('/api/tags')
def get_all_tags():
       stmt = 'SELECT * FROM tag_counts'
       response_list = get_data(stmt, [])
       jsonresponse = [{"id": x.id, "name": x.name, "count": x.count} for x in response_list]
       return jsonify(tags=jsonresponse)

@app.route('/api/livetags')
def get_live_tags():
       stmt = 'SELECT * FROM live_tag_counts'
       response_list = get_data(stmt, [])
       jsonresponse = [{"name": x.name, "count": x.count} for x in response_list]
       return jsonify(tags=jsonresponse)

@app.route('/api/live/posts/experts')
def get_live_post_experts():
       #hourval = datetime.now().strftime('%Y.%m.%d %H')
       #hourval = '2017.02.05 20'
       stmt = "SELECT group_val from posts_data where table_name = 'live_posts_experts_by_hour' LIMIT 1"
       response = session.execute(stmt, parameters=[])
       for val in response:
          hourval = val.group_val
       stmt = 'SELECT * FROM live_posts_experts_by_hour WHERE group_hour =' + "'" + hourval + "'"
       response_list = get_data(stmt, [])
       jsonresponse = [{"id": x.id, "creation_date": x.creation_date, "title": x.title, "experts": x.experts, "tags": x.tags} for x in response_list]
       return jsonify(posts=jsonresponse)



@app.route('/tags')
def tag():
       return render_template("base.html")

@app.route('/chart/tag')
def chart_tag():
 return render_template("charttag.html")

@app.route('/chart/taglive')
def chart_taglive():
 return render_template("charttaglive.html")

