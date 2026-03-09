package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"

	"redis-go-workshop/config"

	"github.com/redis/go-redis/v9"
)

var ctx = context.Background()
var rdb *redis.Client

func main() {
	// Load configuration
	cfg := config.Load()

	// Initialize Redis client
	rdb = redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", cfg.RedisHost, cfg.RedisPort),
		Password: "",
		DB:       0,
	})

	http.HandleFunc("/", homeHandler)
	http.HandleFunc("/ping", pingHandler)
	http.HandleFunc("/set/", setHandler)
	http.HandleFunc("/get/", getHandler)

	serverAddr := fmt.Sprintf(":%s", cfg.ServerPort)
	fmt.Printf("Server running on http://localhost%s\n", serverAddr)
	log.Fatal(http.ListenAndServe(serverAddr, nil))
}

func homeHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"message": "Redis Go Workshop - Ready!",
	})
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	pong, err := rdb.Ping(ctx).Result()
	if err != nil {
		json.NewEncoder(w).Encode(map[string]string{
			"status":  "error",
			"message": err.Error(),
		})
		return
	}

	json.NewEncoder(w).Encode(map[string]string{
		"status": "connected",
		"ping":   pong,
	})
}

func setHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	key := r.URL.Path[len("/set/"):]
	body, err := io.ReadAll(r.Body)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]string{
			"status":  "error",
			"message": err.Error(),
		})
		return
	}

	err = rdb.Set(ctx, key, string(body), 0).Err()
	if err != nil {
		json.NewEncoder(w).Encode(map[string]string{
			"status":  "error",
			"message": err.Error(),
		})
		return
	}

	json.NewEncoder(w).Encode(map[string]interface{}{
		"status": "success",
		"key":    key,
		"value":  string(body),
	})
}

func getHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	key := r.URL.Path[len("/get/"):]
	value, err := rdb.Get(ctx, key).Result()
	if err != nil {
		json.NewEncoder(w).Encode(map[string]string{
			"status":  "error",
			"message": err.Error(),
		})
		return
	}

	json.NewEncoder(w).Encode(map[string]interface{}{
		"status": "success",
		"key":    key,
		"value":  value,
	})
}
