#!/bin/bash

# nginx 리버스 프록시 관리 스크립트

set -e

# 도움말 출력 함수
show_help() {
    echo "📖 Nginx 리버스 프록시 관리 스크립트"
    echo ""
    echo "사용법: $0 [COMMAND]"
    echo ""
    echo "명령어:"
    echo "  start      nginx 컨테이너 시작 (기본값)"
    echo "  stop       nginx 컨테이너 중지"
    echo "  restart    nginx 컨테이너 재시작"
    echo "  rebuild    nginx 이미지 재빌드 후 시작"
    echo "  logs       nginx 로그 실시간 확인"
    echo "  status     nginx 컨테이너 상태 확인"
    echo "  clean      nginx 컨테이너 및 볼륨 완전 삭제"
    echo "  help       이 도움말 출력"
    echo ""
    echo "예시:"
    echo "  $0 start"
    echo "  $0 rebuild"
    echo "  $0 logs"
}

# 네트워크 확인 및 생성 함수
ensure_network() {
    if ! docker network ls | grep -q "cutline-network"; then
        echo "📡 Creating cutline-network..."
        docker network create cutline-network
    fi
}

# nginx 시작 함수
start_nginx() {
    echo "🚀 Starting Nginx Reverse Proxy..."
    ensure_network
    echo "🌐 Starting nginx container..."
    docker-compose up -d
    echo "✅ Nginx is now running on http://localhost"
    echo "🌐 Swagger UI: http://localhost/swagger-ui/"
    echo "📋 To view logs: $0 logs"
    echo "🛑 To stop: $0 stop"
}

# nginx 중지 함수
stop_nginx() {
    echo "🛑 Stopping Nginx..."
    docker-compose down
    echo "✅ Nginx stopped successfully!"
}

# nginx 재시작 함수
restart_nginx() {
    echo "🔄 Restarting Nginx..."
    stop_nginx
    start_nginx
}

# nginx 재빌드 함수
rebuild_nginx() {
    echo "🔨 Rebuilding Nginx..."
    docker-compose down
    docker-compose pull nginx
    echo "🌐 Starting nginx with fresh image..."
    ensure_network
    docker-compose up -d --force-recreate
    echo "✅ Nginx rebuilt and started successfully!"
    echo "🌐 Swagger UI: http://localhost/swagger-ui/"
}

# 로그 확인 함수
show_logs() {
    echo "📋 Showing Nginx logs (Ctrl+C to exit)..."
    docker-compose logs -f nginx
}

# 상태 확인 함수
show_status() {
    echo "📊 Nginx Container Status:"
    docker-compose ps
    echo ""
    echo "🌐 Network Status:"
    docker network ls | grep cutline-network || echo "❌ cutline-network not found"
    echo ""
    echo "🔗 Access URLs:"
    echo "  - Main: http://localhost"
    echo "  - Swagger UI: http://localhost/swagger-ui/"
    echo "  - API Docs: http://localhost/api-docs"
    echo "  - Health: http://localhost/health"
}

# 완전 정리 함수
clean_nginx() {
    echo "🧹 Cleaning up Nginx completely..."
    echo "⚠️  This will remove containers, images, and volumes!"
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down -v --remove-orphans
        docker-compose rm -f
        docker volume prune -f
        echo "✅ Nginx cleaned up successfully!"
    else
        echo "❌ Operation cancelled."
    fi
}

# 메인 로직
case "${1:-start}" in
    start)
        start_nginx
        ;;
    stop)
        stop_nginx
        ;;
    restart)
        restart_nginx
        ;;
    rebuild)
        rebuild_nginx
        ;;
    logs)
        show_logs
        ;;
    status)
        show_status
        ;;
    clean)
        clean_nginx
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo "❌ Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac
