FROM apache/superset

USER root

#RUN pip install apache-superset[druid]
RUN pip install pydruid

USER superset

COPY ./superset_config.py /app/pythonpath/superset_config.py

RUN superset fab create-admin --username admin --firstname Superset --lastname Admin --email admin@superset.com --password admin 
RUN superset db upgrade
#RUN superset load_examples
RUN superset init

