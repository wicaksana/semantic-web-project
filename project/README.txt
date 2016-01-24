The web app uses Flask.

To run the app:
1. Install Fuseki Server (https://jena.apache.org/documentation/serving_data/#download-fuseki1)
2. run Fuseki Server using ./fuseki-server in extracted directory of the Fuseki software
3. open a browser, go to http://localhost:3030
4. click  'manage datasets', click 'add new dataset', give name 'wolff2' (otherwise you should modify file views.py to align with your config)
5. Upload movie.rdf to Fuseki
6. move to folder 'web-app'
7. Install the python dependencies urllib3, python2-beautifulsoup4, sparqlwrapper and flask
8. run the app using Python 2 (python or python2 depending on OS):
	$ python run.py
