Purpose:

The input of the system is semantic data that is extracted from the web. One of the
best sources of structured data is http://webdatacommons.org. But the data needs cleanup
and is not consumable as-is by most of the semantic frameworks (eg: Jena) or triplestores
(eg: allegrograph, stardog). This is a simple python script, that cleans up the nquads
data, making it consumable.

Usage:

Assumption: The webcommons data is downloaded and is available in the local file system. Let's call this 'recipe.nquads'

Create a python virutalenv

Use pip install to install required packages:

* pip install -r requirements.txt

Run the python script

* python cleanup_webcommonsdata.py recipe.nquads output.nquads
