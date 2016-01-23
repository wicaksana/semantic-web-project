The web app uses Flask.

To run the app:
1. Install Fuseki Server (https://jena.apache.org/documentation/serving_data/#download-fuseki1)
2. run Fuseki Server
3. open a browser, go to http://localhost:3030
4. click  'manage datasets', click 'add new dataset', give name 'wolff2' (otherwise you should modify file views.py to align with your config)
5. Upload wolff.rdf to Fuseki
6. move to folder 'web-app'
7. run the app:
	$ python run.py
