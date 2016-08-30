To point to war in dev ($DSSRC) env directement from prod ($DS) env
0. do not copy rtbf-rest directory or war to $DS/webapps
1. place devdd.d in $DS/webapps
2. move devdd.d/rtbf-rest.xml directement in $DS/webapps

Access it http://localhost:8070/rs
