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
# from oauth2client import client, crypt

from google.oauth2 import id_token
from google.auth.transport import requests

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
]


def check_auth(request, isAppRequest=False):
    """
        Checks if the current user is authenticated
        Returns the current user, the url and the url_link_text
    """
    app_connection = False;

    if isAppRequest:
        token = request.get("userToken")
        try:
            idinfo = id_token.verify_oauth2_token(token, requests.Request(), '671966008147-5ai21s9elm6bml9rsmkmbvvrdr4i3l16.apps.googleusercontent.com')
            userEmail = idinfo['email']
        except:
            userEmail = request.get("userEmail")
        return userEmail, "", "", True

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
        all_group_members = list(set(person_obj.family_group_members + person_obj.friends_group_members + person_obj.other_group_members))
        for member_id in all_group_members:
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

        template_values = {
            'navigation': NAV_LINKS,
            'user': current_user,
            'page_header': "TrackerApp",
            'tracked_user_obj': person_obj,
            'tracked_user_id': person_key_str,
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
        groups = ["Family", "Friends", "Others"]

        group_members = {}
        group_members["Family"] = []
        for member_id in person_obj.family_group_members:
            member_person = Person.query(Person.user_id == member_id).get()
            group_members["Family"].append(member_person)
        group_members["Friends"] = []
        for member_id in person_obj.friends_group_members:
            member_person = Person.query(Person.user_id == member_id).get()
            group_members["Friends"].append(member_person)
        group_members["Others"] = []
        for member_id in person_obj.other_group_members:
            member_person = Person.query(Person.user_id == member_id).get()
            group_members["Others"].append(member_person)


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
    def post(self, group_str):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)

        if current_user is None:
            self.redirect("/auth")
            return
        person_obj = Person.get_by_user(current_user)

        added_person_key_str = self.request.get("addUser")
        added_person_key = ndb.Key(urlsafe=added_person_key_str)
        added_person = added_person_key.get()

        if (group_str == "Family"):
            if len(list(person_obj.family_group_members)):
                members = [p for p in person_obj.family_group_members]
                members.append(added_person.user_id)
                person_obj.family_group_members = list(set(members))
            else:
                members = []
                members.append(added_person.user_id)
                person_obj.family_group_members = list(set(members))
        elif (group_str == "Friends"):
            if len(list(person_obj.friends_group_members)):
                members = [p for p in person_obj.friends_group_members]
                members.append(added_person.user_id)
                person_obj.friends_group_members = list(set(members))
            else:
                members = []
                members.append(added_person.user_id)
                person_obj.friends_group_members = list(set(members))
        elif (group_str == "Others"):
            if len(list(person_obj.other_group_members)):
                members = [p for p in person_obj.other_group_members]
                members.append(added_person.user_id)
                person_obj.other_group_members = list(set(members))
            else:
                members = []
                members.append(added_person.user_id)
                person_obj.other_group_members = list(set(members))

        person_obj.put()
        self.redirect('/groups')
# [END GroupsPage]

#[START PostDroidLoc]
class PostDroidLoc(webapp2.RequestHandler):
    def post(self):
        user_email, auth_url, url_link_text, app_connection = check_auth(self.request, True)
        lat = self.request.get("lat")
        lon = self.request.get("lon")

        if user_email is None:
            self.redirect("/auth")
            return

        person_obj = Person.get_by_user_email(user_email)

        locPt = LocationPoint(
                tracked_person = person_obj.user_id,
                tracked_location=ndb.GeoPt(lat,lon),
        )
        locPt.put()
        #lat = 30.0
        #lon = -97.0

        # self.response.headers['Content-Type'] = 'application/json'
        self.response.out.write(str(lat) + " " + str(lon) )
#[END PostDroidLoc]

# [START GroupsDroid]
class GroupsDroid(webapp2.RequestHandler):
    def get(self):
        user_email, auth_url, url_link_text, app_connection = check_auth(self.request, True)
        if user_email is None:
            self.redirect("/auth")
            return

        person_obj = Person.get_by_user_email(user_email)
        groups = ["Family", "Friends", "Others"]

        group_members = {}
        group_members["Family"] = []
        for member_id in person_obj.family_group_members:
            member_person = Person.query(Person.user_id == member_id).get()

            # self.response.out.write(str(str(member_person)))
            # self.response.out.write(str(json.dumps('\n\n')))
            # self.response.out.write(str(str(member_person.email)))
            # self.response.out.write(str(json.dumps('\n\n')))
            # return

            tracked_person = LocationPoint.get_by_owner_person(member_person)
            tracked_location_list = LocationPoint.query(LocationPoint.tracked_person == member_id).order(-LocationPoint.tracked_time).fetch(1)
            if len(tracked_location_list):
                lat = tracked_location_list[0].tracked_location.lat
                lon = tracked_location_list[0].tracked_location.lon
            else:
                lat = ""
                lon = ""
            group_members["Family"].append({
                "email" : member_person.email,
                "name" : member_person.name,
                "phone" : member_person.phone_number,
                "lat" : lat, 
                "lon" : lon  
            })

        group_members["Friends"] = []
        for member_id in person_obj.friends_group_members:
            member_person = Person.query(Person.user_id == member_id).get()

            tracked_location_list = LocationPoint.query(LocationPoint.tracked_person == member_id).order(-LocationPoint.tracked_time).fetch(1)
            if len(tracked_location_list):
                lat = tracked_location_list[0].tracked_location.lat
                lon = tracked_location_list[0].tracked_location.lon
            else:
                lat = ""
                lon = ""
            group_members["Friends"].append({
                "email" : member_person.email,
                "name" : member_person.name,
                "phone" : member_person.phone_number,
                "lat" : lat,
                "lon" : lon
            })

        group_members["Others"] = []
        for member_id in person_obj.other_group_members:
            member_person = Person.query(Person.user_id == member_id).get()
            tracked_location_list = LocationPoint.query(LocationPoint.tracked_person == member_id).order(-LocationPoint.tracked_time).fetch(1)
            if len(tracked_location_list):
                lat = tracked_location_list[0].tracked_location.lat
                lon = tracked_location_list[0].tracked_location.lon
            else:
                lat = ""
                lon = ""
            group_members["Others"].append({
                "email" : member_person.email,
                "name" : member_person.name,
                "phone" : member_person.phone_number,
                "lat" : lat,
                "lon" : lon
            })

        groupsJson = { 'groups': groups, 'groupsMembers': group_members }

        self.response.headers['Content-Type'] = 'application/json'
        self.response.out.write(str(json.dumps(groupsJson)))

    # This will me used to add user to Group only
    def post(self, group_str):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)

        if current_user is None:
            self.redirect("/auth")
            return
        person_obj = Person.get_by_user(current_user)

        added_person_key_str = self.request.get("addUser")
        added_person_key = ndb.Key(urlsafe=added_person_key_str)
        added_person = added_person_key.get()

        if (group_str == "Family"):
            if len(list(person_obj.family_group_members)):
                members = [p for p in person_obj.family_group_members]
                members.append(added_person.user_id)
                person_obj.family_group_members = list(set(members))
            else:
                members = []
                members.append(added_person.user_id)
                person_obj.family_group_members = list(set(members))
        elif (group_str == "Friends"):
            if len(list(person_obj.friends_group_members)):
                members = [p for p in person_obj.friends_group_members]
                members.append(added_person.user_id)
                person_obj.friends_group_members = list(set(members))
            else:
                members = []
                members.append(added_person.user_id)
                person_obj.friends_group_members = list(set(members))
        elif (group_str == "Others"):
            if len(list(person_obj.other_group_members)):
                members = [p for p in person_obj.other_group_members]
                members.append(added_person.user_id)
                person_obj.other_group_members = list(set(members))
            else:
                members = []
                members.append(added_person.user_id)
                person_obj.other_group_members = list(set(members))

        person_obj.put()
        self.redirect('/groups')

# [END GroupsDroid]

#  [START SingleDroidLoc]
class SingleDroidLoc(webapp2.RequestHandler):
    def get(self):
        user_email, auth_url, url_link_text, app_connection = check_auth(self.request, True)
        if user_email is None:
            self.redirect("/auth")
            return

        person_obj = Person.get_by_user_email(user_email)

        trackeeObj = Person.get_by_user_email(self.request.get("trackeeEmail"))

        tracked_location_list = LocationPoint.query(LocationPoint.tracked_person == trackeeObj.user_id).order(-LocationPoint.tracked_time).fetch(1)
        if len(tracked_location_list):
            lat = tracked_location_list[0].tracked_location.lat
            lon = tracked_location_list[0].tracked_location.lon
        else:
            lat = ""
            lon = ""
        #lat =  30.0 + random.uniform(0, .09)
        #lon = -97.0 + random.uniform(0, .09)

        location = { "trackee" : trackeeObj.email, 'lat': lat, 'lon': lon }

        self.response.headers['Content-Type'] = 'application/json'
        self.response.out.write(str(json.dumps(location)))

# [END SingleDroidLoc]

class GetTracked(webapp2.RequestHandler):
    def get(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)
        person_obj = Person.get_by_user(current_user)

        locations = []
        all_group_members = list(set(person_obj.family_group_members + person_obj.friends_group_members + person_obj.other_group_members))
        for member_id in all_group_members:
            person = Person.query(Person.user_id == member_id).get()
            location_list = LocationPoint.query(LocationPoint.tracked_person == member_id).order(-LocationPoint.tracked_time).fetch(1)
            if len(location_list):
                locations.append({
                    'user':person.email,
                    'lat':location_list[0].tracked_location.lat,
                    'lon':location_list[0].tracked_location.lon
                })
        self.response.out.write(json.dumps(locations))


# [START AlertsPage]
class AlertsPage(webapp2.RequestHandler):

    def get(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)
        if current_user is None:
            self.redirect("/auth")
            return
        person_obj = Person.get_by_user(current_user)
        alerts_query = LocationAlert.query(
            LocationAlert.alert_owner == str(person_obj.user_id)
        )
        alerts = alerts_query.fetch()
        

        template_values = {
            'navigation': NAV_LINKS,
            'page_header': "TrackerApp",
            'current_user': current_user,
            'person_obj': person_obj,
            'alerts': alerts,
            'auth_url': auth_url,
            'url_link_text': url_link_text,
        }
        template = JINJA_ENVIRONMENT.get_template('alerts_page.html')
        self.response.write(template.render(template_values))
# [END AlertsPage]

# [START AlertCreate]
class AlertCreate(webapp2.RequestHandler):

    def get(self):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)
        if current_user is None:
            self.redirect("/auth")
            return
        person_obj = Person.get_by_user(current_user)
       
        all_users = Person.query().fetch()

        blank_alert = LocationAlert(
            alert_name = "",
            alert_tracked_people = [],
            alert_location_center = ndb.GeoPt(0,0), 
            alert_location_radius= 0,
        )

        template_values = {
            'navigation': NAV_LINKS,
            'page_header': "TrackerApp",
            'current_user': current_user,
            'person_obj': person_obj,
            'edit': False,
            'alert': blank_alert,
            'all_users': all_users,
            'auth_url': auth_url,
            'url_link_text': url_link_text,
        }
        template = JINJA_ENVIRONMENT.get_template('alert_form_page.html')
        self.response.write(template.render(template_values))
# [END AlertCreate]

# [START AlertEdit]
class AlertEdit(webapp2.RequestHandler):

    def get(self, alert_key_str):
        current_user, auth_url, url_link_text, app_connection = check_auth(self.request)
        if current_user is None:
            self.redirect("/auth")
            return
        person_obj = Person.get_by_user(current_user)
       
        alert_key = ndb.Key(urlsafe=alert_key_str)
        alert_obj = alert_key.get()

        all_users = Person.query().fetch()

        template_values = {
            'navigation': NAV_LINKS,
            'page_header': "TrackerApp",
            'current_user': current_user,
            'edit': True,
            'alert': alert_obj,
            'person_obj': person_obj,
            'all_users': all_users,
            'auth_url': auth_url,
            'url_link_text': url_link_text,
        }
        template = JINJA_ENVIRONMENT.get_template('alert_form_page.html')
        self.response.write(template.render(template_values))
# [END AlertEdit]

# [START ToggleAlert]
class ToggleAlert(webapp2.RequestHandler):
    def post(self, alert_key_str):
        alert_key = ndb.Key(urlsafe=alert_key_str)
        alert_obj = alert_key.get()
        alert_obj.alert_status = not(alert_obj.alert_status)
        alert_obj.put()
        self.response.out.write(json.dumps({"success":True}))

# [END ToggleAlert]

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
    ('/groups', GroupsPage),
    ('/groups/(.+)', GroupsPage),
    ('/groupsDroid', GroupsDroid),
    ('/groupsDroid/(.+)',GroupsDroid),
    ('/postLocation', PostDroidLoc),
    ('/postLocation/(.+)', PostDroidLoc),
    ('/singleDroid', SingleDroidLoc),
    ('/singleDroid/(.+)', SingleDroidLoc),
    ('/get_tracked', GetTracked),
    ('/alerts',AlertsPage),
    ('/toggle_alert/(.+)', ToggleAlert),
    ('/alert_create', AlertCreate),
    ('/alert_edit/(.+)', AlertEdit),

    # REST interface (Person example, use any Model)
    # GET all people: /rest/people    returns list of all Person objects in "results" key
    # GET a particular Person:  /rest/people/<person ID>
    # GET filtered list of people: /rest/people?q=<GQL query>  
    #    Example: /rest/people?q=email%3D%27kelseyking511@gmail.com%27
    #

    
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
        
    # Get list of locations by user_id:
    #/rest/locations?q=tracked_person%3D%27{{ user_id }}%27&order=tracked_time
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
        LocationAlert, 
        permissions={
            'GET': PERMISSION_ANYONE,
            'POST': PERMISSION_ANYONE,
            'PUT': PERMISSION_ANYONE,
            'DELETE': PERMISSION_ANYONE
        },
        # Will be called for every PUT, right before the model is saved (also supports GET/POST/DELETE)
        #put_callback=lambda model, data: model
    ),
], config=config, debug=True)
# [END app]
