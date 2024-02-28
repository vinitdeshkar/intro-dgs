package com.example.soundtracks.datafetchers;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.example.soundtracks.models.MappedPlaylist;
import java.util.List;
import com.example.soundtracks.datasources.SpotifyClient;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.soundtracks.models.PlaylistCollection;
import com.netflix.graphql.dgs.InputArgument;
import com.netflix.graphql.dgs.DgsMutation;
import com.example.soundtracks.generated.types.AddItemsToPlaylistInput;
import com.example.soundtracks.generated.types.AddItemsToPlaylistPayload;
import com.example.soundtracks.models.Snapshot;
import com.example.soundtracks.generated.types.Playlist;
import java.util.Objects;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;

@DgsComponent
public class PlaylistDataFetcher {

    private final SpotifyClient spotifyClient;

    @Autowired
    public PlaylistDataFetcher(SpotifyClient spotifyClient) {
        this.spotifyClient = spotifyClient;
    }

    @DgsQuery
    public  List<MappedPlaylist> featuredPlaylists() {
        PlaylistCollection response = spotifyClient.featuredPlaylistsRequest();
        return response.getPlaylists();
    }

    @DgsQuery
    public MappedPlaylist playlist(@InputArgument String id) {
        return spotifyClient.playlistRequest(id);
    }

    @DgsMutation
    public AddItemsToPlaylistPayload addItemsToPlaylist(@InputArgument AddItemsToPlaylistInput input) {

        String playlistId = input.getPlaylistId();
        List<String> uris = input.getUris();
        Snapshot snapshot = spotifyClient.addItemsToPlaylist(playlistId, String.join(",", uris));
        AddItemsToPlaylistPayload payload = new AddItemsToPlaylistPayload();

        if (snapshot != null) {
            String snapshotId = snapshot.id();
            if (Objects.equals(snapshotId, playlistId)) {
                Playlist playlist = new Playlist();
                playlist.setId(playlistId);
                payload.setCode(200);
                payload.setMessage("success");
                payload.setSuccess(true);
                payload.setPlaylist(playlist);
                return payload;
            }
        }

        payload.setCode(500);
        payload.setMessage("could not update playlist");
        payload.setSuccess(false);
        payload.setPlaylist(null);

        return payload;
    };

    @DgsData(parentType="AddItemsToPlaylistPayload", field="playlist")
    public MappedPlaylist getPayloadPlaylist(DgsDataFetchingEnvironment dfe) {

        AddItemsToPlaylistPayload payload = dfe.getSource();
        Playlist playlist = payload.getPlaylist();

        if (playlist != null) {
            String playlistId = playlist.getId();
            return spotifyClient.playlistRequest(playlistId);
        }

        return null;
    }
}