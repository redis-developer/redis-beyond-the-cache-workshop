import redis
from flask import Flask, jsonify, request
from config import Config

app = Flask(__name__)

# Redis connection
redis_client = redis.Redis(
    host=Config.REDIS_HOST,
    port=Config.REDIS_PORT,
    db=Config.REDIS_DB,
    decode_responses=True
)

@app.route('/')
def home():
    return jsonify({"message": "Redis Python Workshop - Ready!"})

@app.route('/ping')
def ping():
    try:
        response = redis_client.ping()
        return jsonify({"status": "connected", "ping": response})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})

@app.route('/set/<key>', methods=['POST'])
def set_value(key):
    try:
        value = request.get_data(as_text=True)
        redis_client.set(key, value)
        return jsonify({"status": "success", "key": key, "value": value})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})

@app.route('/get/<key>')
def get_value(key):
    try:
        value = redis_client.get(key)
        return jsonify({"status": "success", "key": key, "value": value})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})

if __name__ == '__main__':
    app.run(debug=Config.FLASK_DEBUG, port=Config.FLASK_PORT)

