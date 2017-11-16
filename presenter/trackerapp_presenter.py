#!/usr/bin/env python

# [START imports]
import os
import urllib
import logging
import time
import json
import random

import jinja2
import webapp2

from rest_gae import *
from rest_gae.users import UserRESTHandler

from pprint import pprint
from math import sin, cos, sqrt, atan2, radians
from google.appengine.api import users
from google.appengine.ext import ndb
from google.appengine.ext.db import Key
from models.trackerapp_models import *
from google.appengine.api import app_identity
from google.appengine.api import mail
from datetime import datetime

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader('templates'),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)
# [END imports]

REPORT_RATE_MINUTES = "0"
LAST_REPORT = None
SECS_PER_HOUR = 3600

NAV_LINKS = [
    {"label": "Home", "link": "/home"},
    {"label": "Groups", "link": "/groups"},
    {"label": "Alerts", "link": "/alerts"},
    {"label": "Followers", "link": "/followers"},
]

def check_auth(request, isAppRequest=False):
    """
        Checks if the current user is authenticated
        Returns the current user, the url and the url_link_text
    """
    app_connection = False;

    if isAppRequest:
        app_connection = True
        current_user = Person(email=request.get("userEmail"))
        return current_user, "", "", app_connection

    current_user = users.get_current_user()
    if current_user:
        auth_url = users.create_logout_url(request.uri)
        url_link_text = 'Logout'
        person_obj = Person.get_by_user(current_user)
        if person_obj is None:
            #TODO: get full name from google profile?
            person = Person(
                user_id = current_user.user_id(),
                email = current_user.email(),
            )
            p_key = person.put()
    else:
        auth_url = users.create_login_url(request.uri)
        url_link_text = 'Login'
    return current_user, auth_url, url_link_text, app_connection


# [START UserAuthentication]
class Auth(webapp2.RequestHandler):

    def get(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)

        if current_user:
            # Proceed to Create stream page if user exists
            template_values = {
                'navigation': NAV_LINKS,
                'user': current_user,
                'page_header': "TrackerApp",
                ### 'stream_name_label': Stream.stream_name._verbose_name,
                'auth_url': auth_url,
                'url_link_text': url_link_text,
            }

            template = JINJA_ENVIRONMENT.get_template('home_page.html')
            self.response.write(template.render(template_values))
        else:
            # Return the login page
            template_values = {
                'page_header': "Welcome to TrackerApp!",
                'auth_url': auth_url,
                'url_link_text': url_link_text,
            }

            template = JINJA_ENVIRONMENT.get_template('auth.html')
            self.response.write(template.render(template_values))

    def post(self):
        submit_url = "/home"

        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)

        tp = users.User(self.request.get("userEmail"))
        
        self.redirect(submit_url)
# [END UserAuthentication]

# [START HomePage]
class HomePage(webapp2.RequestHandler):
    def get(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)

        if current_user is None:
            self.redirect("/auth")
            return

        tracked_people = []
        locations = []
        person_obj = Person.get_by_user(current_user)
        groups_query = Group.query(
            Group.group_owner == person_obj.user_id
        )
        groups = groups_query.fetch()
        for group in groups:
            for member_id in group.group_members:
                person = Person.query(Person.user_id == member_id).get()
                tracked_people.append(person)
                location_list = LocationPoint.query(LocationPoint.tracked_person == member_id).order(-LocationPoint.tracked_time).fetch(1)
                if len(location_list):
                    locations.append({
                        'user':person.email,
                        'lat':location_list[0].tracked_location.lat,
                        'lon':location_list[0].tracked_location.lon
                    })

        template_values = {
            'navigation': NAV_LINKS,
            'user': current_user,
            'page_title': "TrackerApp",
            'page_header': "TrackerApp",
            'tracked_people': tracked_people,
            'locations': locations,
            'auth_url': auth_url,
            'url_link_text': url_link_text,
        }

        template = JINJA_ENVIRONMENT.get_template('home_page.html')
        self.response.write(template.render(template_values))
# [END HomePage]

# [START ErrorPage]
class ErrorPage(webapp2.RequestHandler):

    def get(self):
        template = JINJA_ENVIRONMENT.get_template('error.html')
        self.response.write(template.render())
# [END ErrorPage]

# [START RetracePage]
class RetracePage(webapp2.RequestHandler):

    def get(self, person_key_str):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)
        if current_user is None:
            self.redirect("/auth")
            return

        person_obj = Person.query(Person.user_id == person_key_str).get()

        person_location_query = LocationPoint.query(
            LocationPoint.tracked_person == current_user.user_id() 
        )
        location_points = person_location_query.fetch()
        sorted_location_points = sorted(location_points, key=lambda s: s.tracked_time)

        template_values = {
            'navigation': NAV_LINKS,
            'user': current_user,
            'page_header': "TrackerApp",
            'tracked_user_obj': person_obj,
            'location_points': sorted_location_points,
            'auth_url': auth_url,
            'url_link_text': url_link_text,
        }
        template = JINJA_ENVIRONMENT.get_template('retrace_page.html')
        self.response.write(template.render(template_values))
# [END RetracePage]

# [START GroupsPage]
class GroupsPage(webapp2.RequestHandler):

    def get(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)
        if current_user is None:
            self.redirect("/auth")
            return
        person_obj = Person.get_by_user(current_user)
        groups_query = Group.query(
            Group.group_owner == str(person_obj.user_id)
        )
        groups = groups_query.fetch()
        
        group_members = {}
        for group in groups:
            group_members[group.key.urlsafe()] = []
            for member_id in group.group_members:
                member_person = Person.query(Person.user_id == member_id).get()
                group_members[group.key.urlsafe()].append(member_person)

        all_users = Person.query().fetch()

        template_values = {
            'navigation': NAV_LINKS,
            'page_header': "TrackerApp",
            'current_user': current_user,
            'person_obj': person_obj,
            'groups': groups,
            'group_members': group_members,
            'all_users': all_users,
            'auth_url': auth_url,
            'url_link_text': url_link_text,
        }
        template = JINJA_ENVIRONMENT.get_template('groups_page.html')
        self.response.write(template.render(template_values))

    # This will me used to add user to Group only
    def post(self, group_key_str):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)

        if current_user is None:
            self.redirect("/auth")
            return
        person_obj = Person.get_by_user(current_user)

        group_key = ndb.Key(urlsafe=group_key_str)
        group_obj = group_key.get()
        
        added_person_key_str = self.request.get("addUser")
        added_person_key = ndb.Key(urlsafe=added_person_key_str)
        added_person = added_person_key.get()

        if len(list(group_obj.group_members)):
            members = [p for p in group_obj.group_members]
            members.append(added_person.user_id)
            group_obj.group_members = members
        else:
            members = []
            members.append(added_person.user_id)
            group_obj.group_members = members

        group_obj.put()
        self.redirect('/groups')
# [END GroupsPage]

class GetTracked(webapp2.RequestHandler):
    def get(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)
        person_obj = Person.get_by_user(current_user)

        locations = []
        groups_query = Group.query(
            Group.group_owner == person_obj.user_id
        )
        groups = groups_query.fetch()
        for group in groups:
            for member_id in group.group_members:
                person = Person.query(Person.user_id == member_id).get()
                location_list = LocationPoint.query(LocationPoint.tracked_person == member_id).order(-LocationPoint.tracked_time).fetch(1)
                if len(location_list):
                    locations.append({
                        'user':person.email,
                        'lat':location_list[0].tracked_location.lat,
                        'lon':location_list[0].tracked_location.lon
                    })
        self.response.out.write(json.dumps(locations))


config = {}
config['webapp2_extras.sessions'] = {
    'secret_key': 'my-super-secret-key',
}

# [START app]
app = webapp2.WSGIApplication([
    ('/', HomePage),
    ('/auth', Auth),
    ('/home', HomePage),
    ('/error',ErrorPage),
    ('/retrace/(.+)',RetracePage),
    ('/groups',GroupsPage),
    ('/groups/(.+)',GroupsPage),
    ('/get_tracked', GetTracked),

    # REST interface (Person example, use any Model)
    # GET all people: /rest/people    returns list of all Person objects in "results" key
    # GET a particular Person:  /rest/people/<person ID>
    # GET filtered list of people: /rest/people?q=<GQL query>  
    #    Example: /rest/people?q=email%3D%27kelseyking511@gmail.com%27
    #
    #TODO: add docs on POST,PUT,DELETE

    
    # REST endpoints reference:
    #   /rest/people
    #   /rest/groups
    #   /rest/locations
    #   /rest/alerts
    RESTHandler(
        '/rest/people', # The base URL for this model's endpoints
        Person, # The model to wrap
        permissions={
            'GET': PERMISSION_ANYONE,
            'POST': PERMISSION_ANYONE,
            'PUT': PERMISSION_ANYONE,
            'DELETE': PERMISSION_ANYONE
        },
        # Will be called for every PUT, right before the model is saved (also supports GET/POST/DELETE)
        #put_callback=lambda model, data: model
    ),
    RESTHandler(
        '/rest/groups',
        Group, 
        permissions={
            'GET': PERMISSION_ANYONE,
            'POST': PERMISSION_ANYONE,
            'PUT': PERMISSION_OWNER_USER,
            'DELETE': PERMISSION_ANYONE
        },
        #post_callback=
    ),
    RESTHandler(
        '/rest/locations',
        LocationPoint, 
        permissions={
            'GET': PERMISSION_ANYONE,
            'POST': PERMISSION_ANYONE,
            'PUT': PERMISSION_ANYONE,
            'DELETE': PERMISSION_ANYONE
        },
    ),
    RESTHandler(
        '/rest/alerts', 
        Group, 
        permissions={
            'GET': PERMISSION_ANYONE,
            'POST': PERMISSION_ANYONE,
            'PUT': PERMISSION_OWNER_USER,
            'DELETE': PERMISSION_OWNER_USER
        },
        # Will be called for every PUT, right before the model is saved (also supports GET/POST/DELETE)
        #put_callback=lambda model, data: model
    ),
], config=config, debug=True)
# [END app]
