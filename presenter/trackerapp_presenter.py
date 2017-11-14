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
        person_obj = Person.get_by_user(current_user)
        groups_query = Group.query(
            Group.group_owner == person_obj
        )
        groups = groups_query.fetch()
        for group in groups:
            for member in group.group_members:
                tracked_people.append(member)

        template_values = {
            'navigation': NAV_LINKS,
            'user': current_user,
            'page_title': "TrackerApp",
            'page_header': "TrackerApp",
            'tracked_people': tracked_people,
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

        person_key = ndb.Key(urlsafe=person_key_str)
        person_obj = person_key.get()

        person_location_query = LocationPoint.query(
            LocationPoint.person == Person(
                email=current_user.email()
            )
            # TODO: Is this better? -- test w/ data
            #LocationPoint.person == person_obj
        )
        location_points = person_location_query.fetch()
        sorted_location_points = sorted(location_points, key=lambda s: s.tracked_time)

        template_values = {
            'navigation': NAV_LINKS,
            'user': current_user,
            'page_header': "TrackerApp",
            'tracked_user_key': user_key_str,
            'tracked_user_obj': user_obj,
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
            Group.group_owner == person_obj
        )
        groups = groups_query.fetch()

        all_users = Person.query().fetch()

        template_values = {
            'navigation': NAV_LINKS,
            'page_header': "TrackerApp",
            'current_user': current_user,
            'person_obj': person_obj,
            'groups': groups,
            'all_users': all_users,
            'auth_url': auth_url,
            'url_link_text': url_link_text,
        }
        template = JINJA_ENVIRONMENT.get_template('groups_page.html')
        self.response.write(template.render(template_values))

    # This will me used to add a group only
    def post(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)

        if current_user is None:
            self.redirect("/auth")
            return
        person_obj = Person.get_by_user(current_user)

        group = Group(
            group_members=[],
            group_owner=person_obj,
            group_name=self.request.get('group_name'),
        )

        group_key = group.put()
        self.redirect('/groups')
# [END GroupsPage]


# [START app]
app = webapp2.WSGIApplication([
    ('/', HomePage),
    ('/auth', Auth),
    ('/home', HomePage),
    ('/error',ErrorPage),
    ('/retrace/(.+)',RetracePage),
    ('/groups',GroupsPage),

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
            'DELETE': PERMISSION_OWNER_USER
        },
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
], debug=True)
# [END app]
