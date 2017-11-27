#!/usr/bin/env python

# [START imports]
import os
import urllib

from google.appengine.api import users
from google.appengine.ext import ndb
from google.appengine.ext import blobstore 
from google.appengine.api import images
from google.appengine.api import search

import jinja2
import webapp2

from pprint import pprint

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)
# [END imports]

# [START Person]
class Person(ndb.Model):
    user_id = ndb.StringProperty()
    email = ndb.StringProperty()
    name = ndb.StringProperty()
    phone_number = ndb.StringProperty()
    family_group_members = ndb.StringProperty(repeated=True)
    friends_group_members = ndb.StringProperty(repeated=True)
    other_group_members = ndb.StringProperty(repeated=True)

    @classmethod
    def get_by_user(cls, user):
        try:
            return cls.query().filter(cls.user_id == user.user_id()).get()
        except:
            return cls.query().filter(cls.user_id == user).get()

    @classmethod
    def get_by(cls):
        return cls.query()

# [END Person]


# [START LocationPoint]
class LocationPoint(ndb.Model):
    tracked_person = ndb.StringProperty()
    tracked_time = ndb.DateTimeProperty(auto_now_add=True)
    tracked_location = ndb.GeoPtProperty(verbose_name="location")

    @classmethod
    def get_by_owner_user(cls, user):
        return cls.query().filter(cls.tracked_person == user.user_id()).get()
# [END LocationPoint]


# [START LocationAlert]
class LocationAlert(ndb.Model):
    alert_status = ndb.BooleanProperty(default=True)
    alert_name = ndb.StringProperty(required=True, verbose_name="Name your alert") 
    alert_owner = ndb.StringProperty()
    alert_tracked_people = ndb.StringProperty(repeated=True, verbose_name="People to Track")
    alert_location_center = ndb.GeoPtProperty()
    alert_location_radius = ndb.FloatProperty()
    alert_day_sun = ndb.BooleanProperty(default=False)
    alert_day_mon = ndb.BooleanProperty(default=False)
    alert_day_tue = ndb.BooleanProperty(default=False)
    alert_day_wed = ndb.BooleanProperty(default=False)
    alert_day_thu = ndb.BooleanProperty(default=False)
    alert_day_fri = ndb.BooleanProperty(default=False)
    alert_day_sat = ndb.BooleanProperty(default=False)
    alert_time_start = ndb.TimeProperty()
    alert_time_end = ndb.TimeProperty()
    # direction: False = outward, True = inward
    alert_direction = ndb.BooleanProperty(default=False)

    class RESTMeta:
        user_owner_property = 'alert_owner'
# [END LocationAlert]

    @classmethod
    def get_by_owner_user(cls, user):
        return cls.query().filter(cls.alert_owner == user.user_id()).get()

    @classmethod
    def get_by_member_user(cls, user):
        return cls.query().filter(user.user_id() in cls.alert_tracked_people).get()

