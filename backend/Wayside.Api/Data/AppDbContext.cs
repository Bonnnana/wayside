using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using Wayside.Api.Entities;

namespace Wayside.Api.Data;

public sealed class AppDbContext(DbContextOptions<AppDbContext> options)
    : IdentityDbContext<WaysideUser, IdentityRole<Guid>, Guid>(options)
{
    public DbSet<StoredPlace> Places => Set<StoredPlace>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<StoredPlace>(place =>
        {
            place.HasKey(p => p.Id);
            place.Property(p => p.Id).HasMaxLength(255);
            place.Property(p => p.Name).HasMaxLength(200);
            // Names, not ordinals — reordering PlaceCategory would otherwise re-map every
            // stored place silently.
            place.Property(p => p.Category).HasConversion<string>().HasMaxLength(32);
            place.Property(p => p.OpenUntil).HasMaxLength(16);
            // Computed from the stored JSON; nothing to persist for them.
            place.Ignore(p => p.Tags);
            place.Ignore(p => p.ReviewThemes);
            place.Ignore(p => p.Photos);
        });

        builder.Entity<WaysideUser>(user =>
        {
            user.Property(u => u.FirstName).HasMaxLength(100);
            user.Property(u => u.LastName).HasMaxLength(100);
            // Refresh tokens are looked up by value on every /auth/refresh call.
            user.HasIndex(u => u.RefreshToken);
        });
    }
}
