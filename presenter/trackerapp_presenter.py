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
    logging.info(current_user)
    if current_user:
        auth_url = users.create_logout_url(request.uri)
        url_link_text = 'Logout'
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
                'url': submit_url,
            }

            template = JINJA_ENVIRONMENT.get_template('create_stream.html')
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
        # TODO Use this userEmail and Password to sign in
        
        self.redirect(submit_url)
# [END UserAuthentication]

# [START HomePage]
class HomePage(webapp2.RequestHandler):
    def get(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)

        if current_user is None:
            self.redirect("/auth")
            return

        #TODO: Fetch Tracked People
        tracked_people = []

        template_values = {
            'navigation': NAV_LINKS,
            'user': current_user,
            'page_title': "TrackerApp",
            'page_header': "TrackerApp Home",
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

# [START app]
app = webapp2.WSGIApplication([
    ('/', Auth),
    ('/home', HomePage),
    ('/error',ErrorPage),
], debug=True)
# [END app]
