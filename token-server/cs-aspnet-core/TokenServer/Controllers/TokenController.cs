using System;
using System.Collections.Generic;
using System.IdentityModel.Tokens.Jwt;
using System.Linq;
using System.Net.Http;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text.Json;
using System.Threading.Tasks;
using System.Web;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Microsoft.IdentityModel.Tokens;
using TokenServer.Models;

namespace TokenServer.Controllers
{
    [ApiController]
    public class TokenController : ControllerBase
    {
        private static readonly SecurityKey securityKey = new RsaSecurityKey(new RSACryptoServiceProvider(2048).ExportParameters(true));
        private static readonly HttpClient httpClient = new HttpClient(new SocketsHttpHandler
        {
            MaxConnectionsPerServer = int.MaxValue,
        });

        [HttpPost("/token")]
        public async Task<TokenCreateResponse> Create([FromBody] TokenCreateRequest request)
        {
            var identity = await GetIdentityAsync(request.UserId);
            var claims = new[] {
                new Claim("sub", request.UserId),
                new Claim("given_name", identity.FirstName),
                new Claim("family_name", identity.LastName),
                new Claim("iat", DateTimeOffset.Now.ToUnixTimeSeconds().ToString(), ClaimValueTypes.Integer64),
            };
            var signingCredentials = new SigningCredentials(securityKey, SecurityAlgorithms.RsaSha256);

            var token = new JwtSecurityToken(claims: claims, signingCredentials: signingCredentials);
            return new TokenCreateResponse
            {
                Token = new JwtSecurityTokenHandler().WriteToken(token),
            };
        }

        private async Task<IdentityGetResponse> GetIdentityAsync(string userId)
        {
            var uriBuilder = new UriBuilder("http://localhost:9000/user");
            var query = HttpUtility.ParseQueryString(uriBuilder.Query);
            query["userId"] = userId;
            uriBuilder.Query = query.ToString();

            var response = await httpClient.GetAsync(uriBuilder.Uri, HttpCompletionOption.ResponseHeadersRead);
            response.EnsureSuccessStatusCode();

            return await JsonSerializer.DeserializeAsync<IdentityGetResponse>(
                await response.Content.ReadAsStreamAsync(),
                new JsonSerializerOptions
                {
                    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
                });
        }
    }
}
