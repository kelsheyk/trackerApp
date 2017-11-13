#!/usr/bin/env python

import os, random
from pprint import pprint
from google.appengine.api import users
from google.appengine.ext import ndb
from google.appengine.ext.db import Key
from models.trackerapp_models import *
from google.appengine.api import app_identity
from google.appengine.api import mail
import datetime


# This script creates 5 dummy users, & their locations
# TO LOAD INTO DATASTORE:
# 1. Launch a development server:       dev_appserver.py ./
# 2. Go to admin interactive console:   http://127.0.0.1:8000/console
# 3. Copy/paste this file into console and execute.
# NOTE: Executing this file more than once will create duplicate users.

mary = Person(email='mary@dummyUser.com')
bob = Person(email='bob@dummyUser.com')
joe = Person(email='joe@dummyUser.com')
jane = Person(email='jane@dummyUser.com')
larry = Person(email='larry@dummyUser.com')

people = []
people.append(mary)
people.append(bob)
people.append(joe)
people.append(jane)
people.append(larry)

people_keys = {}
people_keys['mary'] = mary.put()
people_keys['bob'] = bob.put()
people_keys['joe'] = joe.put()
people_keys['jane'] = jane.put()
people_keys['larry'] = larry.put()


# Now their locations
now = datetime.datetime.now()
d = datetime.timedelta(days = 3)
start_time = now - d

# stub a location every 15 mins
freq = datetime.timedelta(minutes = 15)
# Generate a random location point constrained to 30,-97 (for simplicity)
latitude = 30.2
longitude = -97.7

current_time = start_time
while current_time < now:
    for person in people:
        lat = latitude + random.random()/10
        lon = longitude + random.random()/10
        current_time = current_time + freq
        loc = LocationPoint(
            tracked_person=person,
            tracked_time=current_time,
            tracked_location=ndb.GeoPt(lat,lon)
        )
        loc.put()


