-- Run this once against your PostgreSQL instance before starting services
CREATE DATABASE restromind;

\c restromind;

CREATE SCHEMA IF NOT EXISTS restromind_auth;
CREATE SCHEMA IF NOT EXISTS restromind_restaurant;
CREATE SCHEMA IF NOT EXISTS restromind_menu;
CREATE SCHEMA IF NOT EXISTS restromind_order;
CREATE SCHEMA IF NOT EXISTS restromind_user;
CREATE SCHEMA IF NOT EXISTS restromind_notification;
CREATE SCHEMA IF NOT EXISTS restromind_analytics;
