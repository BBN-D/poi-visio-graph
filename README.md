poi-visio-graph
===============

poi-visio-graph is a Java library that loads Visio OOXML (vsdx) files
using the poi-visio library and creates an in-memory graph structure
from the objects present on the page. Not only does it utilize
user-specified connection points, but it performs analysis to infer
logical visual connection points between the objects on each page.
One possible use of this library is to create a Network diagram from a
Visio document.

Parses a visio document and creates a connected graph from it, inferring
connections where necessary.

Building
========

This depends on the poi-visio library, which can be found at

    https://github.com/BBN-D/poi-visio

Once that is installed, you can build this just like any other maven project:

    mvn install

Legal
=====

This code has been approved by DARPA's Public Release Center for
public release, with the following statement:

* Approved for Public Release, Distribution Unlimited

Copyright & License
-------------------

Copyright (c) 2015 Raytheon BBN Technologies Corp

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

