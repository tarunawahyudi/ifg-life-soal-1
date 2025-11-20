#!/bin/bash

echo "🚀 STOPPING EVERYTHING..."
docker compose down -v

echo "🗑️ CLEANING VOLUMES..."
docker volume rm ifg-life-soal-1_postgres_data 2>/dev/null || true

echo "🔥 STARTING FRESH..."
docker compose up -d

echo "⏳ WAITING FOR POSTGRES..."
sleep 10

echo "✅ CHECKING STATUS:"
docker compose ps