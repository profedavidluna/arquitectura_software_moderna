namespace ProductService.Infrastructure.Cache;

using System.Text.Json;
using StackExchange.Redis;

public interface IRedisCacheService
{
    Task<T?> GetAsync<T>(string key);
    Task SetAsync<T>(string key, T value, TimeSpan? expiry = null);
    Task RemoveAsync(string key);
}

public class RedisCacheService : IRedisCacheService
{
    private readonly IDatabase? _database;
    private readonly ILogger<RedisCacheService> _logger;

    public RedisCacheService(IConfiguration configuration, ILogger<RedisCacheService> logger)
    {
        _logger = logger;
        var connectionString = configuration["Redis:ConnectionString"];

        if (!string.IsNullOrEmpty(connectionString))
        {
            try
            {
                var redis = ConnectionMultiplexer.Connect(new ConfigurationOptions
                {
                    EndPoints = { connectionString },
                    AbortOnConnectFail = false,
                    ConnectTimeout = 3000
                });
                _database = redis.GetDatabase();
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Redis connection failed. Running without cache.");
            }
        }
    }

    public async Task<T?> GetAsync<T>(string key)
    {
        if (_database == null) return default;

        try
        {
            var value = await _database.StringGetAsync(key);
            if (value.IsNullOrEmpty) return default;
            return JsonSerializer.Deserialize<T>(value!);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Redis GET failed for key {Key}", key);
            return default;
        }
    }

    public async Task SetAsync<T>(string key, T value, TimeSpan? expiry = null)
    {
        if (_database == null) return;

        try
        {
            var json = JsonSerializer.Serialize(value);
            await _database.StringSetAsync(key, json, expiry ?? TimeSpan.FromMinutes(5));
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Redis SET failed for key {Key}", key);
        }
    }

    public async Task RemoveAsync(string key)
    {
        if (_database == null) return;

        try
        {
            await _database.KeyDeleteAsync(key);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Redis DELETE failed for key {Key}", key);
        }
    }
}
