FROM apache/superset

USER root

#RUN pip install apache-superset[druid]
RUN pip install pydruid

USER superset

RUN superset fab create-admin --username admin --firstname Superset --lastname Admin --email admin@superset.com --password superset
RUN superset db upgrade
#RUN superset load_examples
RUN superset init
