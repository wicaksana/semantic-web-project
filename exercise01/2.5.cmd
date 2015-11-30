mkdir triple-store\
tdbloader.bat --loc .\triple-store\ .\royal92.nt
tdbquery.bat --loc=triple-store\ --query=q_2_1_a.rq --syntax ARQ --results text